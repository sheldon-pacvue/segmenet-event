package com.pacvue.segment.event.extend;

import com.segment.analytics.MessageTransformer;
import com.segment.analytics.messages.MessageBuilder;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

public interface ReactorMessageTransformer extends MessageTransformer {
    Mono<Boolean> transform(MessageBuilder<?, ?> builder, ContextView ctx);

    @Override
    default boolean transform(MessageBuilder builder) {
        throw new UnsupportedOperationException("Use transform(MessageBuilder, ContextView) instead.");
    }
}