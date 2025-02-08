package com.pacvue.segment.event.service.entity.dto;

import cn.hutool.core.date.DateUtil;
import com.segment.analytics.messages.Message;
import lombok.Data;

import java.util.Date;

@Data
public class ResendSegmentEventDTO {
    // 是否强制发送，如果为true，已经发送过的仍然会再次发送
    private Boolean focus = false;
    // createdAt > from
    private Date from = DateUtil.offsetDay(new Date(), -2);
    // createAt < to
    private Date to = new Date();
    // 事件类型
    private String type = Message.Type.track.name();
    // 操作类型
    private Integer operation;
}
