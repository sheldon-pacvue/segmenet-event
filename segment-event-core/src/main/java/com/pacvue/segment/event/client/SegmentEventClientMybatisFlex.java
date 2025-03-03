package com.pacvue.segment.event.client;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import reactor.core.publisher.Mono;

import com.mybatisflex.core.BaseMapper;
import reactor.core.scheduler.Schedulers;

import javax.sql.DataSource;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

@Builder
@Slf4j
public class SegmentEventClientMybatisFlex<T> extends AbstractBufferSegmentEventClient<T, SegmentEventClientMybatisFlex<T>> {
    @Builder.Default
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    @NonNull
    private final SqlSessionFactory sqlSessionFactory;
    @NonNull
    private final Class<? extends BaseMapper<T>> mapperClass;
    @Builder.Default
    private boolean isSupportValues = true;

    @Override
    public Mono<Boolean> send(List<T> events) {
        return Mono.fromCallable(() -> {
            try (SqlSession sqlSession = sqlSessionFactory.openSession(isSupportValues ? ExecutorType.SIMPLE : ExecutorType.BATCH)) {
                BaseMapper<T> mapper = sqlSession.getMapper(mapperClass);
                int result = 0;
                if (isSupportValues) {
                    result = mapper.insertBatch(events);
                } else {
                    events.forEach(mapper::insert);
                    // 执行批处理并提交
                    result = sqlSession.flushStatements().stream()
                            .flatMapToInt(br -> Arrays.stream(br.getUpdateCounts())) // 展开所有 updateCounts
                            .map(uc -> uc == Statement.SUCCESS_NO_INFO ? 1 : uc)     // 处理 SUCCESS_NO_INFO
                            .reduce(result, Integer::sum);                                // 累加所有值
                }
                log.debug("Batch insert completed: {} records inserted", result);
                return result == events.size();
            } catch (Exception ex) {
                log.error("Batch insert failed", ex);
                return Boolean.FALSE;
            }
        });
    }
}
