package com.pacvue.segment.event.entity;


import com.segment.analytics.messages.Message;
import lombok.ToString;

import java.util.Date;
import java.util.Map;


@ToString
final class AutoValue_SegmentDataBaseMessage extends SegmentPersistingMessage {
    private final Message message;
    private final boolean result;
    private final String reportApp;
    private final int operation;
    private final String secret;

    AutoValue_SegmentDataBaseMessage(Message message, boolean result, String reportApp, int operation, String secret) {
        this.message = message;
        this.result = result;
        this.reportApp = reportApp;
        this.operation = operation;
        this.secret = secret;
    }

    @Override
    public Message message() {
        return message;
    }

    @Override
    public boolean result() {
        return result;
    }

    @Override
    public String reportApp() {
        return reportApp;
    }

    @Override
    public int operation() {
        return operation;
    }

    @Override
    public Type type() {
        return message.type();
    }

    @Override
    public String messageId() {
        return message.messageId();
    }

    @Override
    public Date sentAt() {
        return message.sentAt();
    }

    @Override
    public Date timestamp() {
        return message.timestamp();
    }

    @Override
    public Map<String, ?> context() {
        return message.context();
    }

    @Override
    public String anonymousId() {
        return message.anonymousId();
    }

    @Override
    public String userId() {
        return message.userId();
    }

    @Override
    public Map<String, Object> integrations() {
        return message.integrations();
    }

    @Override
    public String secret() {
        return secret;
    }
}
