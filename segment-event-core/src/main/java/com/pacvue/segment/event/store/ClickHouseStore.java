package com.pacvue.segment.event.store;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.pacvue.segment.event.entity.SegmentPersistingMessage;
import com.segment.analytics.messages.Message;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.sql.*;

import static com.pacvue.segment.event.entity.SegmentPersistingMessage.LOG_OPERATION_SEND_TO_SEGMENT;


@Data
@RequiredArgsConstructor
@Accessors(chain = true)
@Slf4j
public class ClickHouseStore implements Store<SegmentPersistingMessage> {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final DataSource dataSource;
    private final String tableName;
    private final long loopIntervalMinutes;
    private MasterElection masterElection;
    private boolean subscribing = false;


    @Override
    public Mono<Boolean> publish(SegmentPersistingMessage event) {
        try {
            return Mono.defer(() -> {
                try {
                    return Mono.just(insertData(event));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            return Mono.error(e);
        }
    }


    /**
     * 从数据库中查询，并且自动上报未成功上报数据
     * 注意分布式问题，如果未实现master竞争机制，则无法使用该方法，使用zookeeper，redis等其他分布式系统进行竞争master
     * 需要循环进行查询，直到查询不到数据,睡眠5分钟，然后重试
     */
    @Override
    public void subscribe(Consumer<List<Message>> consumer, int bundleCount) {
        if (subscribing) {
            return;
        }
        this.subscribing = true;
        loopGetData(consumer);
    }

    @Override
    public void stopScribe() {
        this.subscribing = false;
    }

    @Override
    public void shutdown() {
        this.subscribing = false;
    }

    private void loopGetData(Consumer<List<Message>> consumer) {
        executor.schedule(() -> {
            try {
                if (null == masterElection || !masterElection.isMaster()) {
                    return;
                }
                List<Message> events = queryData();
                log.info("form db data size: {}", events.size());
                if (events.isEmpty()) {
                    return;
                }
                consumer.accept(events);
            } catch (Exception ex) {
                log.warn("resend segment event meet some error", ex);
            } finally {
                if (subscribing) {
                    loopGetData(consumer);
                }
            }
        }, loopIntervalMinutes, TimeUnit.MINUTES);
    }

    /**
     * 这里由于使用zookeeper进行分片和备份，不能直接使用CREATE TABLE IF NOT EXISTS
     * 因为这个sql仅会判断本地表是否存在，即使存在仍然会创建zookeeper节点
     * 但是由于节点已经存在，则会报错，所以分开先判断表是否存在
     */
    public void createTableIfNotExists() {
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

    private boolean insertData(SegmentPersistingMessage event) {
        // 插入的SQL语句
        String insertSQL = """
            INSERT INTO %s (eventDate, hash, userId, type, message, result, operation, createdAt, eventTime)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.formatted(tableName);  // 用表名替换占位符

        try (PreparedStatement preparedStatement = dataSource.getConnection().prepareStatement(insertSQL)) {
            String json = gson.toJson(event);
            // 插入多条记录
            preparedStatement.setDate(1, Optional.ofNullable(DateUtil.date(event.sentAt())).map(DateTime::toSqlDate).orElse(null));  // eventDate
            preparedStatement.setString(2, DigestUtil.md5Hex(json));  // hash
            preparedStatement.setString(3, event.userId());  // userId
            preparedStatement.setString(4, event.type().name());  // type
            preparedStatement.setString(5, json);  // message
            preparedStatement.setBoolean(6, event.result());  // result (UInt8)
            preparedStatement.setInt(7, event.operation());  // operation
            preparedStatement.setLong(8, DateUtil.date().toTimestamp().getTime());  // createdAt
            preparedStatement.setLong(9, Optional.ofNullable(DateUtil.date(event.sentAt())).map(DateTime::toTimestamp).map(Timestamp::getTime).orElse(0L));  // eventTime

            boolean result = preparedStatement.execute();
            log.debug("Data inserted successfully!, result: {}", result);
            return result;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<Message> queryData() throws Exception {
        String querySQL = """
                SELECT MAX(eventDate), hash, MAX(type) as type, MAX(userId) as userId, SUM(result) as result,
                        MAX(message) as message, MIN(eventTime) as eventTime
                FROM %s
                WHERE eventDate >= toDate(now() - INTERVAL 2 DAY)
                AND operation = %d
                GROUP BY hash
                HAVING result = 0
                ORDER BY eventTime
                LIMIT 200
                """.formatted(tableName, LOG_OPERATION_SEND_TO_SEGMENT);

        try (Statement statement = dataSource.getConnection().createStatement();
            ResultSet resultSet = statement.executeQuery(querySQL)) {
            List<Message> list = new ArrayList<>();
            while (resultSet.next()) {
                Message event = JSONUtil.toBean(resultSet.getString("message"), Message.class);
                list.add(event);
            }
            return list;
        }
    }
}
