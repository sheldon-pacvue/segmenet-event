package com.pacvue.segment.event.store;

import com.segment.analytics.messages.Message;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public interface Store<T extends Message>  {
    // 默认方法，如果没有传入 optional，可以使用默认值
    Mono<Boolean> publish(T event);

    void subscribe(Consumer<List<Message>> consumer, int bundleCount);

    void stopScribe();

    void shutdown();
}
