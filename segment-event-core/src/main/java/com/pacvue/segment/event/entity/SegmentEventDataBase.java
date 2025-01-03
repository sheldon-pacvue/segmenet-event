package com.pacvue.segment.event.entity;

import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.pacvue.segment.event.generator.SegmentEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Accessors(chain = true)
public class SegmentEventDataBase<T extends SegmentEvent> extends SegmentEvent {
    public final static int LOG_OPERATION_SEND_TO_DISTRIBUTED_STORE = 1;
    public final static int LOG_OPERATION_SEND_TO_SEGMENT = 2;

    // 消息主体
    private final T message;
    // 是否上报成功
    private final boolean result;
    // 上报给了队列，还是上报给了平台,由于这里改用了rabbitmq，会自动降级到上报平台，所以这个值基本没用了
    private int operation = LOG_OPERATION_SEND_TO_SEGMENT;
    private long createdAt = System.currentTimeMillis();

    @Override
    public String getType() {
        return this.message.getType();
    }

    public long getEventTime() {
        return this.message.getTimestamp();
    }

    public String getEventDate() {
        return DateUtil.date(this.message.getTimestamp()).toDateStr();
    }

    public String getHash() {
        return DigestUtil.md5Hex(JSONUtil.toJsonStr(message));
    }
}