package com.pacvue.segment.event.spring.transformer;

import com.pacvue.segment.event.spring.filter.ServletRequestHolder;
import com.pacvue.segment.event.transformer.ReactorMessageTransformer;
import com.pacvue.segment.event.transformer.ReactorMessageTransformerAdapter;
import com.segment.analytics.MessageTransformer;
import com.segment.analytics.messages.MessageBuilder;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * 配合ServletRequestHolderFilter使用
 */
public class UserIdMessageTransformer implements MessageTransformer {
    private final String key;

    public UserIdMessageTransformer(String key) {
        this.key = key;
    }

    @Override
    public boolean transform(MessageBuilder builder) {
        HttpServletRequest servletRequest = (HttpServletRequest) ServletRequestHolder.get();
        String value = servletRequest.getHeader(key);
        if (value != null) {
            builder.userId(value);
        }
        return true;
    }

    public ReactorMessageTransformer toReact() {
        return new ReactorMessageTransformerAdapter(this);
    }
}
