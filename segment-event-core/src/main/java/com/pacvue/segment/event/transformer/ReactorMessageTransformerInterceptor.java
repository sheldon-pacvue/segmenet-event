package com.pacvue.segment.event.transformer;

import com.segment.analytics.MessageTransformer;
import com.segment.analytics.messages.MessageBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.ContextView;

public class ReactorMessageTransformerInterceptor implements ReactorMessageTransformer {
    private final MessageTransformer delegate;

    public ReactorMessageTransformerInterceptor(MessageTransformer delegate) {
        this.delegate = delegate;
    }

    @Override
    public Mono<Boolean> transform(MessageBuilder<?, ?> builder, ContextView ctx) {
        return Mono.fromCallable(() -> delegate.transform(builder))
                   .subscribeOn(Schedulers.boundedElastic()); // 确保不会阻塞 Reactor 线程
    }
}