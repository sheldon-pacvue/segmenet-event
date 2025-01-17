package com.pacvue.segment.event.spring.transformer;

import com.pacvue.segment.event.spring.filter.ServletRequestHolder;
import com.pacvue.segment.event.extend.ReactorMessageTransformer;
import com.pacvue.segment.event.extend.ReactorMessageTransformerAdapter;
import com.segment.analytics.MessageTransformer;
import com.segment.analytics.messages.MessageBuilder;

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
