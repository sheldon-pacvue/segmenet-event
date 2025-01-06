package com.pacvue.segment.event.store;

import cn.hutool.json.JSONUtil;
import com.pacvue.segment.event.entity.SegmentEventDataBase;
import com.pacvue.segment.event.generator.SegmentEvent;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.sql.*;

@Builder
@Slf4j
public class ClickHouseStore<T extends SegmentEvent> implements Store<SegmentEventDataBase<T>> {
    private final DataSource dataSource;
    private final String tableName;

    public ClickHouseStore(DataSource dataSource, String tableName) {
        this.dataSource = dataSource;
        this.tableName = tableName;
        createTableIfNotExists(this.dataSource);
    }

    /**
     * TODO 写入数据库
     */
    @Override
    public Mono<Boolean> publish(SegmentEventDataBase<T> event) {
        try {
            Connection connection = dataSource.getConnection();
            return Mono.defer(() -> {
                try {
                    return Mono.just(insertData(connection, List.of(event)));
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
     *
     * 未发送成功的数据拿出来再发一次
     * $query = 'SELECT MAX(eventDate), hash, MAX(type) as type, MAX(userId) as userId, SUM(result) as result, '
     *             . ' MAX(message) as message, MIN(eventTime) as eventTime FROM ' . SegmentEventsLog::tableName()
     *             . ' WHERE eventDate >= \'' . $dateFrom . '\' '
     *             . ' AND operation = ' . SegmentIo::LOG_OPERATION_SEND_TO_SEGMENT
     *             . ' GROUP BY hash HAVING result = 0 ORDER BY eventTime';
     *
     * 没搞懂这个有什么用？
     * 将在sqs队列中的数据拿出来重发,发完后删除队列的中数据
     * $query = 'SELECT * FROM ' . SegmentEventsLog::tableName()
     *             . ' WHERE eventDate = \'' . $date . '\' AND type = \'track\''
     *             . ' AND operation = ' . SegmentIo::LOG_OPERATION_SEND_TO_SQS;
     */
    @Override
    public void subscribe(Consumer<List<SegmentEventDataBase<T>>> consumer, int bundleCount) {

    }

    @Override
    public void shutdown() {

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
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean insertData(Connection connection, List<SegmentEventDataBase<T> > events) throws Exception {
        // 插入的SQL语句
        String insertSQL = """
            INSERT INTO %s (eventDate, hash, userId, type, message, result, operation, createdAt, eventTime)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.formatted(tableName);  // 用表名替换占位符

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
            // 插入多条记录
            for (SegmentEventDataBase<T> event : events) {
                preparedStatement.setDate(1, Date.valueOf(event.getEventDate()));  // eventDate
                preparedStatement.setString(2, event.getHash());  // hash
                preparedStatement.setString(3, event.getUserId());  // userId
                preparedStatement.setString(4, event.getType());  // type
                preparedStatement.setString(5, JSONUtil.toJsonStr(event.getMessage()));  // message
                preparedStatement.setInt(6, event.isResult() ? 1 : 0);  // result (UInt8)
                preparedStatement.setInt(7, event.getOperation());  // operation
                preparedStatement.setLong(8, event.getCreatedAt());  // createdAt
                preparedStatement.setLong(9, event.getEventTime());  // eventTime
                preparedStatement.addBatch();
            }
            int[] result = preparedStatement.executeBatch();
            log.debug("Data inserted successfully!, result: {}", result);
            return result.length == events.size();
        }
    }

    private List<T> queryData(Connection connection) throws Exception {
        String querySQL = """
                 SELECT MAX(eventDate), hash, MAX(type) as type, MAX(userId) as userId, SUM(result) as result,
                     MAX(message) as message, MIN(eventTime) as eventTime FROM %s
                    WHERE eventDate >= toDate(now() - INTERVAL 7 DAY)
                    AND operation = 1
                    GROUP BY hash HAVING result = 0 ORDER BY eventTime
                """.formatted(tableName);

        try (Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(querySQL)) {
            List<T> list = new ArrayList<>();
            Class<T> clazz = (Class<T>) SegmentEventDataBase.class; // 假设你查询的实体类是 SegmentEventDataBase

            while (resultSet.next()) {
                // 使用反射构造 T 类型对象
                T instance = clazz.getDeclaredConstructor().newInstance();

                // 假设 T 类具有与数据库列对应的字段，使用反射进行赋值
                Field eventDateField = clazz.getDeclaredField("eventDate");
                eventDateField.setAccessible(true);
                eventDateField.set(instance, resultSet.getDate("eventDate"));

                Field hashField = clazz.getDeclaredField("hash");
                hashField.setAccessible(true);
                hashField.set(instance, resultSet.getString("hash"));

                Field userIdField = clazz.getDeclaredField("userId");
                userIdField.setAccessible(true);
                userIdField.set(instance, resultSet.getString("userId"));

                Field typeField = clazz.getDeclaredField("type");
                typeField.setAccessible(true);
                typeField.set(instance, resultSet.getString("type"));

                Field messageField = clazz.getDeclaredField("message");
                messageField.setAccessible(true);
                messageField.set(instance, resultSet.getString("message"));

                Field resultField = clazz.getDeclaredField("result");
                resultField.setAccessible(true);
                resultField.set(instance, resultSet.getInt("result"));

                Field eventTimeField = clazz.getDeclaredField("eventTime");
                eventTimeField.setAccessible(true);
                eventTimeField.set(instance, resultSet.getInt("eventTime"));

                // 将构造的对象添加到列表
                list.add(instance);
            }
            return list;
        }
    }

    private boolean updateData(Connection connection, List<SegmentEventDataBase<T>> events) throws Exception {
        // 更新SQL语句
        String updateSQL = """
            UPDATE ${tableName}
            SET result = 1
            WHERE hash = ? AND userId = ? AND eventDate = ? AND type = ? AND result = ?
        """.replace("${tableName}", tableName);  // 用表名替换占位符

        try (PreparedStatement preparedStatement = connection.prepareStatement(updateSQL)) {
            // 对每个事件进行更新操作
            for (SegmentEventDataBase<T> event : events) {
                preparedStatement.setString(1, event.getHash());  // hash
                preparedStatement.setString(2, event.getUserId());  // userId
                preparedStatement.setDate(3, Date.valueOf(event.getEventDate()));  // userId
                preparedStatement.setString(4, event.getType());  // userId
                preparedStatement.setInt(5, event.isResult() ? 1 : 0);  // result (UInt8)

                // 添加到批处理
                preparedStatement.addBatch();
            }

            // 执行批量更新
            int[] updateCounts = preparedStatement.executeBatch();
            log.debug("Data updated successfully, affected rows: {}", updateCounts.length);
            return true;
        } catch (Exception e) {
            // 异常处理
            log.warn("Failed to update data!", e);
            return false;
        }
    }
}
