package com.pacvue.segment.event.service.entity.dto;

import cn.hutool.core.date.DateUtil;
import lombok.Data;

import java.util.Date;

@Data
public class ResendSegmentEventBody {
    // 是否已经发送成功
    private Boolean result = false;
    // createdAt > from
    private Date from = DateUtil.offsetDay(new Date(), -2);
    // createAt < to
    private Date to = new Date();
    // 事件类型
    private String type = "track";
    // 操作类型
    private Short operation;
}
