package com.pacvue.segment.event.entity;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true, fluent = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY) // 自动检测字段
public class SegmentEventLogMessage {
    // 事件时间
    private Date eventTime;
    // 用户Id
    private String userId;
    // 事件类型
    private String type;
    // 消息hash
    private String hash;
    // 消息负载
    private String message;
    // report结果 0：未发送 1：已发送
    private boolean reported;
    // 上报方式
    private short operation;
    // report时间
    @Builder.Default
    private Date createdAt = new Date();
}