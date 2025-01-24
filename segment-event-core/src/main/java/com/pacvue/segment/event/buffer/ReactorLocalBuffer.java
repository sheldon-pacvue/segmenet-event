package com.pacvue.segment.event.buffer;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

@Builder
@Slf4j
public class ReactorLocalBuffer<T> extends AbstractBuffer<T> {
    @Builder.Default
    @NonNull
    private Sinks.Many<T> sink = Sinks.many().replay().limit(Duration.ofSeconds(30), Schedulers.parallel());
    @Builder.Default
    private final int bufferSize = 5;
    @Builder.Default
    private final int bufferTimeoutSeconds = 10;

    /**
     * 线程安全的发射控制器,解决线程不安全问题
     */
    private final Sinks.EmitFailureHandler concurrentEmitHandler = (signalType, emitResult) -> {
        if (emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED) {
            LockSupport.parkNanos(10_000); // 10微秒级退避
            return true; // 允许重试
        }
        return false; // 其他错误不重试
    };

    @NotNull
    @Override
    public Mono<Boolean> commit(@NotNull T event) {
        return Mono.create((sinkMono) -> {
            try {
                // 使用emitNext的异步发射机制
                sink.emitNext(event, concurrentEmitHandler);
                sinkMono.success(true); // 假设最终成功
            } catch (Exception e) {
                sinkMono.error(e);
            }
        });
    }

    @NotNull
    @Override
    protected StopAccept doAccept(@NotNull Consumer<List<T>> consumer) {
        Disposable disposable = sink.asFlux()
                .bufferTimeout(bufferSize, Duration.ofSeconds(bufferTimeoutSeconds))
                .publishOn(Schedulers.boundedElastic()) // 使用弹性线程池
                .subscribeOn(Schedulers.parallel())
                .subscribe(
                        events -> {
                            try {
                                log.info("consume buffer size: {}", events.size());
                                consumer.accept(events);
                            } catch (Exception e) {
                                log.error("Consumer error", e);
                            }
                        },
                        error -> log.error("Subscription error", error),
                        () -> log.info("Completed")
                );

        return () -> {
            disposable.dispose();
            sink.tryEmitComplete();
            sink = Sinks.many().replay().limit(Duration.ofSeconds(30), Schedulers.parallel());
        };
    }

    @Override
    public void shutdown() {
        sink.tryEmitComplete();
    }
}
