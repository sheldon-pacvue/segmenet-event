package com.pacvue.segment.event.client;

import com.pacvue.segment.event.buffer.Buffer;
import com.pacvue.segment.event.buffer.ReactorLocalBuffer;
import com.pacvue.segment.event.buffer.StopAccept;
import com.segment.analytics.messages.Message;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public abstract class AbstractBufferSegmentEventClient<T, C extends AbstractBufferSegmentEventClient<T, C>> implements BufferSegmentEventClient<T, C> {
    private Buffer<T> buffer = ReactorLocalBuffer.<T>builder().bufferSize(5).bufferTimeoutSeconds(10).build();
    private StopAccept stopAccept;

    protected AbstractBufferSegmentEventClient () {
        startAccept();
    }

    private void startAccept() {
        if (null != this.stopAccept) {
            this.stopAccept.stop();
        }
        this.stopAccept = this.buffer.accept(events -> {
            log.debug("accept events: {}", events);
            sendInternal(events).subscribe();
        });
    }


    @SuppressWarnings("unchecked")
    public C buffer(@NonNull Buffer<T> buffer) {
        this.buffer = buffer;
        startAccept();
        return (C) this;
    }

    @Override
    public Mono<Boolean> send(T...  events) {
        if (null == this.buffer) {
            return Mono.just(Boolean.FALSE);
        }
        return Flux.fromArray(events)
                .flatMap(this.buffer::commit)
                .all(success -> success)
                .defaultIfEmpty(Boolean.TRUE);
    }

    @Override
    public void flush() {
        if (null == this.buffer) {
            return;
        }
        this.buffer.shutdown();
    }

    /**
     * 这里提供默认实现，子类可重写
     */
    protected Mono<Boolean> sendInternal(List<T> events) {
        return send(events);
    }

    protected abstract Mono<Boolean> send(List<T> events);
}
