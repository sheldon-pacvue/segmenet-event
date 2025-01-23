package com.pacvue.segment.event.client;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import reactor.core.publisher.Mono;

import com.mybatisflex.core.BaseMapper;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

@Builder
@Slf4j
public class SegmentEventClientMybatisFlex<T, D> extends AbstractBufferSegmentEventClient<T, SegmentEventClientMybatisFlex<T, D>> {
    @Builder.Default
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    @NonNull
    private final SqlSessionFactory sqlSessionFactory;
    @NonNull
    private final Class<? extends BaseMapper<D>> mapperClass;
    @NonNull
    private final Function<T, D> argumentsConverter;

    @Override
    public Mono<Boolean> send(List<T> events) {
        return Mono.fromCallable(() -> {
            try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
                BaseMapper<D> mapper = sqlSession.getMapper(mapperClass);
                int result = mapper.insertBatch(events.stream().map(argumentsConverter).collect(Collectors.toList()));
                sqlSession.commit();
                log.debug("Batch insert completed: {} records inserted", result);
                return result == events.size();
            } catch (Exception ex) {
                log.error("Batch insert failed", ex);
                return Boolean.FALSE;
            }
        });
    }

    @Override
    public void flush() {

    }
}
