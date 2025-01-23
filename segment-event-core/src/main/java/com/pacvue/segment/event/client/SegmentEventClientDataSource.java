package com.pacvue.segment.event.client;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

@Builder
@Slf4j
public class SegmentEventClientDataSource<T> extends AbstractBufferSegmentEventClient<T, SegmentEventClientDataSource<T>> {
    @Builder.Default
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    @NonNull
    private final DataSource dataSource;
    @NonNull
    private final String insertSql;
    @NonNull
    private final Function<T, Object[]> argumentsConverter;

    @Override
    public Mono<Boolean> send(List<T> events) {
        return Mono.fromCallable(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {

                for (T event : events) {
                    Object[] arguments = argumentsConverter.apply(event);
                    for (int i = 0; i < arguments.length; i++) {
                        setArgument(preparedStatement, i + 1, arguments[i]);
                    }
                    preparedStatement.addBatch(); // 将 SQL 语句添加到批次
                }

                int[] result = preparedStatement.executeBatch(); // 一次性批量执行
                log.debug("Batch insert completed: {} records inserted", result.length);

                return result.length == events.size(); // 确保所有记录都成功插入
            } catch (Exception ex) {
                log.error("Batch insert failed", ex);
                return Boolean.FALSE;
            }
        });
    }

    @Override
    public void flush() {

    }

    private void setArgument(PreparedStatement preparedStatement, int paramIndex, Object arg) throws SQLException {
        if (arg == null) {
            // 根据实际情况选择合适的类型，默认为 VARCHAR
            preparedStatement.setNull(paramIndex, Types.VARCHAR);
            log.debug("Parameter {}: null (SQL Type: VARCHAR)", paramIndex);
        } else if (arg instanceof String) {
            preparedStatement.setString(paramIndex, (String) arg);
            log.debug("Parameter {}: {} (Type: String)", paramIndex, arg);
        } else if (arg instanceof Integer) {
            preparedStatement.setInt(paramIndex, (Integer) arg);
            log.debug("Parameter {}: {} (Type: Integer)", paramIndex, arg);
        } else if (arg instanceof Long) {
            preparedStatement.setLong(paramIndex, (Long) arg);
            log.debug("Parameter {}: {} (Type: Long)", paramIndex, arg);
        } else if (arg instanceof Double) {
            preparedStatement.setDouble(paramIndex, (Double) arg);
            log.debug("Parameter {}: {} (Type: Double)", paramIndex, arg);
        } else if (arg instanceof Boolean) {
            preparedStatement.setBoolean(paramIndex, (Boolean) arg);
            log.debug("Parameter {}: {} (Type: Boolean)", paramIndex, arg);
        } else if (arg instanceof java.util.Date) {
            preparedStatement.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) arg).getTime()));
            log.debug("Parameter {}: {} (Type: Date -> Timestamp)", paramIndex, arg);
        } else {
            // 如果无法推断类型，使用 setObject 作为兜底
            preparedStatement.setObject(paramIndex, arg);
            log.debug("Parameter {}: {} (Type: Object)", paramIndex, arg);
        }
    }
}
