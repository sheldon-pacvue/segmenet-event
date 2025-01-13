package com.pacvue.segment.event.store;

import com.segment.analytics.messages.Message;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

@Builder
@Slf4j
public class ReactorLocalStore extends AbstractStore<Message> {
    @Builder.Default
    private Sinks.Many<Message> sink = Sinks.many().multicast().onBackpressureBuffer();
    @Builder.Default
    private final int bufferSize = 5;
    @Builder.Default
    private final int bufferTimeoutSeconds = 10;

    @NotNull
    @Override
    public Mono<Boolean> commit(@NotNull Message event) {
        Sinks.EmitResult emitResult = sink.tryEmitNext(event);
        if (emitResult.isFailure()) {
            log.debug("event commit failed, event：{}, reason: {}", event, emitResult);
            throw new RuntimeException("publish failed. " + emitResult);
        }
        log.debug("event commit success, event：{}", event);
        return Mono.just(emitResult.isSuccess());
    }

    @NotNull
    @Override
    protected StopAccept doAccept(@NotNull Consumer<List<Message>> consumer) {
        Disposable accepted = sink.asFlux().bufferTimeout(bufferSize, Duration.ofSeconds(bufferTimeoutSeconds)).subscribe(events -> {
                            log.debug("event consume start, events：{}", events);
                            consumer.accept(events);
                        },
                        error -> log.error("Error consuming events", error),
                        () -> log.debug("Event consumption complete."));
        this.isAccepted = true;
        return () -> {
            sink.tryEmitComplete();
            accepted.dispose();
            this.isAccepted = false;
            // 重建新的 sink 实例,避免commit报错
            sink = Sinks.many().multicast().onBackpressureBuffer();
        };
    }

    @Override
    public void shutdown() {
        sink.tryEmitComplete();
    }
}
