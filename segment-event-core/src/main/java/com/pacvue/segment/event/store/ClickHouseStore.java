package com.pacvue.segment.event.store;

import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.pacvue.segment.event.entity.SegmentEventClassRegistry;
import com.pacvue.segment.event.entity.SegmentEventOptional;
import com.pacvue.segment.event.entity.annotation.SegmentEventType;
import com.pacvue.segment.event.generator.SegmentEvent;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.sql.*;

@Builder
@Slf4j
public class ClickHouseStore implements Store<SegmentEventOptional> {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final DataSource dataSource;
    private final String tableName;
    @Builder.Default
    private boolean subscribing = false;

    public ClickHouseStore(DataSource dataSource, String tableName, boolean subscribing) {
        this.dataSource = dataSource;
        this.tableName = tableName;
        this.subscribing = subscribing;
        createTableIfNotExists(this.dataSource);
    }

    /**
     * TODO 写入数据库
     */
    @Override
    public Mono<Boolean> publish(SegmentEvent event, SegmentEventOptional optional) {
        try {
            Connection connection = dataSource.getConnection();
            return Mono.defer(() -> {
                try {
                    return Mono.just(insertData(connection, event, optional));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    /**
     * TODO 从数据库中查询，并且上报，注意分布式问题，使用zookeeper，redis等其他分布式系统进行选举master进行查询上报
     * 需要循环进行查询，直到查询不到数据,睡眠5分钟，然后重试
     */
    @Override
    public void subscribe(Consumer<List<SegmentEvent>> consumer, int bundleCount) {
        this.subscribing = true;
        CompletableFuture.runAsync(() -> {
            while (subscribing) {
                try {
                    List<SegmentEvent> events = queryData(dataSource.getConnection());
                    if (events.isEmpty()) {
                        // 如果没有数据，休眠5分钟
                        TimeUnit.MINUTES.sleep(5);
                    }
                    for (SegmentEvent event : events) {
                        consumer.accept(List.of(event));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, executorService);
    }

    @Override
    public void shutdown() {
        this.subscribing = false;
    }

    /**
     * 这里由于使用zookeeper进行分片和备份，不能直接使用CREATE TABLE IF NOT EXISTS
     * 因为这个sql仅会判断本地表是否存在，即使存在仍然会创建zookeeper节点
     * 但是由于节点已经存在，则会报错，所以分开先判断表是否存在
     */
    private void createTableIfNotExists(DataSource dataSource) {
        String checkTableSQL = "EXISTS TABLE " + tableName;
        String createTableSQL = """
               CREATE TABLE %s (
                   `eventDate` Date,
                   `hash` String,
                   `userId` String,
                   `type` String,
                   `message` String,
                   `result` UInt8,
                   `operation` UInt8,
                   `createdAt` Int32,
                   `eventTime` Int32
               )
               ENGINE = ReplicatedMergeTree('/clickhouse/tables/{shard}/SegmentEventsLog', '{replica}')
               PARTITION BY toYYYYMM(eventDate)
               ORDER BY (userId, eventDate, type, result)
               SETTINGS index_granularity = 8192;
               """.formatted(tableName);

        try (Statement statement = dataSource.getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(checkTableSQL);
            if (resultSet.next() && resultSet.getBoolean(1)) {
                return;
            }
            statement.execute(createTableSQL);
            log.debug("create table success, tableName: {}", tableName);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean insertData(Connection connection, SegmentEvent event, SegmentEventOptional optional) {
        // 插入的SQL语句
        String insertSQL = """
            INSERT INTO %s (eventDate, hash, userId, type, message, result, operation, createdAt, eventTime)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.formatted(tableName);  // 用表名替换占位符

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
            // 插入多条记录
            preparedStatement.setDate(1, DateUtil.date(event.getTimestamp()).toSqlDate());  // eventDate
            preparedStatement.setString(2, DigestUtil.md5Hex(JSONUtil.toJsonStr(event)));  // hash
            preparedStatement.setString(3, event.getUserId());  // userId
            preparedStatement.setString(4, event.getClass().getAnnotation(SegmentEventType.class).value());  // type
            preparedStatement.setString(5, JSONUtil.toJsonStr(event));  // message
            preparedStatement.setInt(6, optional.isResult() ? 1 : 0);  // result (UInt8)
            preparedStatement.setInt(7, optional.getOperation());  // operation
            preparedStatement.setLong(8, DateUtil.date().toTimestamp().getTime());  // createdAt
            preparedStatement.setLong(9, event.getTimestamp());  // eventTime

            boolean result = preparedStatement.execute();
            log.debug("Data inserted successfully!, result: {}", result);
            return result;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<SegmentEvent> queryData(Connection connection) throws Exception {
        String querySQL = """
                SELECT MAX(eventDate), hash, MAX(type) as type, MAX(userId) as userId, SUM(result) as result,
                        MAX(message) as message, MIN(eventTime) as eventTime
                FROM %s
                WHERE eventDate >= toDate(now() - INTERVAL 7 DAY)
                AND operation = 1
                GROUP BY hash
                HAVING result = 0
                ORDER BY eventTime
                LIMIT 100
                """.formatted(tableName);

        try (Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(querySQL)) {
            List<SegmentEvent> list = new ArrayList<>();
            while (resultSet.next()) {
                Class<? extends SegmentEvent> clazz = SegmentEventClassRegistry.getSegmentEventClass(
                        resultSet.getString("type"));
                // 创建对象实例
                SegmentEvent event = clazz.getConstructor().newInstance();

                // 遍历所有字段并进行映射
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    Object value = resultSet.getObject(fieldName);
                    field.set(event, value);
                }
                list.add(event);
            }
            return list;
        }
    }
}
