package com.pacvue.segment.event.store;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class ReactorLocalStore<T> implements Store<T> {
    private final Sinks.Many<T> sink = Sinks.many().multicast().onBackpressureBuffer(); // 外部持有 FluxSink 引用

    @Override
    public Mono<Boolean> publish(T event) {
        Sinks.EmitResult emitResult = sink.tryEmitNext(event);
        return Mono.just(emitResult.isSuccess());
    }

    @Override
    public void subscribe(Consumer<List<T>> consumer, int bundleCount) {
        sink.asFlux().bufferTimeout(bundleCount, Duration.ofSeconds(2)).subscribe(consumer);
    }
}
