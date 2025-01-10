package com.pacvue.segment.event.entity;


import com.segment.analytics.messages.Message;

import java.util.Date;
import java.util.Map;


final class AutoValue_SegmentDataBaseMessage extends SegmentPersistingMessage {
    private final Message message;
    private final boolean result;
    private final int operation;

    AutoValue_SegmentDataBaseMessage(Message message, boolean result, int operation) {
        this.message = message;
        this.result = result;
        this.operation = operation;
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
}
