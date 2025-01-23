package com.pacvue.segment.event.client;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

@Builder
@Slf4j
public class SegmentEventClientMybatisPlus<T, D> extends AbstractBufferSegmentEventClient<T, SegmentEventClientMybatisPlus<T, D>> {
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
                List<BatchResult> result = mapper.insert(events.stream().map(argumentsConverter).collect(Collectors.toList()));
                sqlSession.commit();
                log.debug("Batch insert completed: {} records inserted", result);
                return result.size() == events.size();
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
