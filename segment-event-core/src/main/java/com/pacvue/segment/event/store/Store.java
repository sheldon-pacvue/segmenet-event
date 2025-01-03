package com.pacvue.segment.event.store;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public interface Store<T> {
    Mono<Boolean> publish(T event);

    void subscribe(Consumer<List<T>> consumer, int bundleCount);

    void shutdown();
}
