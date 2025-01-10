package com.pacvue.segment.event.store;

import com.segment.analytics.messages.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class ReactorLocalStore implements Store<Message> {
    private Sinks.Many<Message> sink = Sinks.many().multicast().onBackpressureBuffer(); // 外部持有 FluxSink 引用
    private final int bufferTimeoutSeconds;
    private boolean subscribing = false;

    @Override
    public Mono<Boolean> publish(Message event) {
        Sinks.EmitResult emitResult = sink.tryEmitNext(event);
        if (emitResult.isFailure()) {
            log.debug("event publish failed, event：{}, reason: {}", event, emitResult);
            throw new RuntimeException("publish failed. " + emitResult);
        }
        log.debug("event publish success, event：{}", event);
        return Mono.just(emitResult.isSuccess());
    }

    @Override
    public void subscribe(Consumer<List<Message>> consumer, int bundleCount) {
        if (subscribing) { return; }
        this.subscribing = true;
        sink.asFlux().bufferTimeout(bundleCount, Duration.ofSeconds(bufferTimeoutSeconds))
                .takeWhile(events -> this.subscribing).subscribe(events -> {
            log.debug("event consume start, events：{}", events);
            consumer.accept(events);
        }, error -> {
            log.error("Error consuming events", error);
        }, () -> {
            log.debug("Event consumption complete.");
        });
    }

    /**
     * 1.将缓存快速释放，并且关闭publish
     * 2.停止消费
     * 3.重建缓存，让publish不会报错
     */
    @Override
    public void stopScribe() {
        sink.tryEmitComplete();
        this.subscribing = false;
        sink = Sinks.many().multicast().onBackpressureBuffer();  // 重建新的 sink 实例
    }

    @Override
    public void shutdown() {
        sink.tryEmitComplete();
    }
}
