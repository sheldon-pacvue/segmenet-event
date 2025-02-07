package com.pacvue.segment.event.entity;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.segment.analytics.messages.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true, fluent = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY) // 自动检测字段
public class SegmentEventLogMessage implements MessageLog<SegmentEventLogMessage> {
    // 消息hash
    private String hash;
    // 消息负载
    private Message message;
    // report结果 0：未发送 1：已发送
    private boolean result;
    // 上报方式
    private short operation;
    // log时间
    @Builder.Default
    private Date createdAt = new Date();

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

    @Override
    public Date sentAt() {
        return message.sentAt();
    }

    @NotNull
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
    public SegmentEventLogMessage covert(Message message) {
        return this.message(message)
                .hash(gson.toJson(message))
                .createdAt(new Date());
    }

    @Override
    public SegmentEventLogMessage covert(SegmentEventLogMessage message) {
        return covert(message.message())
                .result(message.result())
                .operation(message.operation());
    }
}