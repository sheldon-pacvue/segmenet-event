package com.pacvue.segment.event.client;

import com.pacvue.segment.event.buffer.Buffer;
import com.pacvue.segment.event.buffer.DefaultBuffer;
import com.pacvue.segment.event.buffer.StopObserver;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
public abstract class AbstractBufferSegmentEventClient<T, C extends AbstractBufferSegmentEventClient<T, C>> implements BufferSegmentEventClient<T, C> {
    protected Buffer<T> buffer = DefaultBuffer.<T>builder().build();
    private StopObserver stopAccept;

    protected AbstractBufferSegmentEventClient () {
        startAccept();
    }

    private void startAccept() {
        if (null != this.stopAccept) {
            this.stopAccept.stop();
        }
        this.stopAccept = this.buffer.observer(events -> {
            log.debug("accept events: {}", events);
            // TODO需要判断是否需要阻塞，避免无限多的线程
            return send(events);
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
                .flatMap(this.buffer::submit)
                .all(success -> success)
                .defaultIfEmpty(Boolean.TRUE);
    }

    @Override
    public void flush() {
        if (null == this.buffer) {
            return;
        }
        this.buffer.flush();
    }

    protected abstract Mono<Boolean> send(List<T> events);
}
