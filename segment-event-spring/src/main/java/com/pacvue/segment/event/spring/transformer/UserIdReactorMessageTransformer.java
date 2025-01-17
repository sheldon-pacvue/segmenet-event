package com.pacvue.segment.event.spring.transformer;

import com.pacvue.segment.event.transformer.ReactorMessageTransformer;
import com.segment.analytics.messages.MessageBuilder;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

/**
 * 配合ReactorRequestHolderFilter使用
 */
public class UserIdReactorMessageTransformer implements ReactorMessageTransformer {
    private final String key;

    public UserIdReactorMessageTransformer(String key) {
        this.key = key;
    }

    @Override
    public Mono<Boolean> transform(MessageBuilder<?, ?> builder, ContextView ctx) {
        ServerWebExchange context = ctx.get(ServerWebExchange.class);
        String value = context.getRequest().getHeaders().getFirst(key);
        if (value != null) {
            builder.userId(value);
        }
        return Mono.just(Boolean.TRUE);
    }
}
