package com.pacvue.segment.event.generator;

import com.segment.analytics.messages.Message;
import com.segment.analytics.messages.MessageBuilder;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface SegmentEventGenerator<T extends Message, V extends MessageBuilder<T, V>>  {
    Mono<MessageBuilder<T, V>> generate();
}
