package com.pacvue.segment.event.store;

import com.pacvue.segment.event.core.SegmentEvent;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public interface Store<T> {
    Mono<Boolean> publish(T event);

    void subscribe(Consumer<List<T>> consumer, int consumeCountPer);
}
