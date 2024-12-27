package com.pacvue.segment.event.store;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class ReactorLocalStore<T> implements Store<T> {
    private final Sinks.Many<T> sink = Sinks.many().multicast().onBackpressureBuffer(); // 外部持有 FluxSink 引用
    private final int bufferTimeoutSeconds;

    @Override
    public Mono<Boolean> publish(T event) {
        Sinks.EmitResult emitResult = sink.tryEmitNext(event);
        return Mono.just(emitResult.isSuccess());
    }

    @Override
    public void subscribe(Consumer<List<T>> consumer, int bundleCount) {
        sink.asFlux().bufferTimeout(bundleCount, Duration.ofSeconds(bufferTimeoutSeconds)).subscribe(events -> {
            try {
                consumer.accept(events);
            } catch (Throwable e) {
                log.error("send data error, events: {}", events, e);
            }
        });
    }
}
