package com.pacvue.segment.event.client;

import com.segment.analytics.messages.Message;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

@Builder
@Slf4j
public class SegmentEventClientClickHouse<T extends Message> implements SegmentEventClient<T> {
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
        return Flux.fromIterable(events)
                .flatMap(event -> {
                    try (PreparedStatement preparedStatement = dataSource.getConnection().prepareStatement(insertSql)) {
                        Object[] arguments = argumentsConverter.apply(event);
                        for (int i = 0; i < arguments.length; i++) {
                            setArgument(preparedStatement, i + 1, arguments[i]);
                        }
                        preparedStatement.execute();
                        log.debug("data inserted successfully!");
                        return Mono.just(Boolean.TRUE);
                    } catch (Exception ex) {
                        return Mono.error(ex);
                    }
                })
                .all(success -> success);
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
