package com.pacvue.segment.event.transformer;

import com.segment.analytics.MessageInterceptor;
import com.segment.analytics.messages.Message;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

public interface ReactorMessageInterceptor extends MessageInterceptor {
    Mono<Message> intercept(Message message, ContextView ctx);

    default Message intercept(Message message) {
        throw new UnsupportedOperationException("Use intercept(Message, ContextView) instead.");
    }
}
