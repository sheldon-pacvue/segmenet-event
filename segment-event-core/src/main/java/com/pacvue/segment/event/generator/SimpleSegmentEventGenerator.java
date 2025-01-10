package com.pacvue.segment.event.generator;

import com.segment.analytics.messages.Message;
import com.segment.analytics.messages.MessageBuilder;
import reactor.core.publisher.Mono;

public abstract class SimpleSegmentEventGenerator<T extends Message, V extends MessageBuilder<T, V>> implements SegmentEventGenerator<T, V> {
    public abstract MessageBuilder<T, V> getObject();

    @Override
    public Mono<MessageBuilder<T, V>> generate() {
        return Mono.defer(() -> Mono.just(getObject()));
    }
}
