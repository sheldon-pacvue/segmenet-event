package com.pacvue.segment.event.service.entity.dto;

import com.segment.analytics.messages.Message;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class SegmentEventLogCursor {
    private String userId;
    private Date eventDate;
    private Message.Type type;
    private Boolean result;
}
