package com.pacvue.segment.event.store;

import com.pacvue.segment.event.core.SegmentEvent;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class ReactorLocalStore implements Store<SegmentEvent> {
    private final Sinks.Many<SegmentEvent> sink = Sinks.many().multicast().onBackpressureBuffer(); // 外部持有 FluxSink 引用

    @Override
    public Mono<Boolean> publish(SegmentEvent event) {
        Sinks.EmitResult emitResult = sink.tryEmitNext(event);
        return Mono.just(emitResult.isSuccess());
    }

    @Override
    public void subscribe(Consumer<List<SegmentEvent>> consumer, int bundleCount) {
        sink.asFlux().take(bundleCount).collectList().subscribe(consumer);
    }
}
