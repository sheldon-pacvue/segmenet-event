package com.pacvue.segment.event.service.entity.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ResendSegmentEventBody {
    private Date from;
    private Date to;
    private String type;
}
