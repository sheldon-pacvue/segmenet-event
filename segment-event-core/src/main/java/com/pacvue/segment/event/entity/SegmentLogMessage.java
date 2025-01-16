package com.pacvue.segment.event.entity;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.segment.analytics.messages.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true, fluent = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY) // 自动检测字段
public final class SegmentLogMessage implements Message {
    public final static int LOG_OPERATION_SEND_TO_SEGMENT = 2;

    private Message message;
    private boolean result;
    private String reportApp;
    private int operation;
    private String secret;

    @NotNull
    @Override
    public Type type() {
        return message.type();
    }

    @NotNull
    @Override
    public String messageId() {
        return message.messageId();
    }

    @Nullable
    @Override
    public Date sentAt() {
        return message.sentAt();
    }

    @NotNull
    @Override
    public Date timestamp() {
        return message.timestamp();
    }

    @Nullable
    @Override
    public Map<String, ?> context() {
        return message.context();
    }

    @Nullable
    @Override
    public String anonymousId() {
        return message.anonymousId();
    }

    @Nullable
    @Override
    public String userId() {
        return message.userId();
    }

    @Nullable
    @Override
    public Map<String, Object> integrations() {
        return message.integrations();
    }
}