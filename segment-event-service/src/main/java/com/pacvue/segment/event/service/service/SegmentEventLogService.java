package com.pacvue.segment.event.service.service;


import com.pacvue.segment.event.service.entity.dto.ResendSegmentEventDTO;
import com.pacvue.segment.event.service.entity.po.SegmentEventLog;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SegmentEventLogService {
    Flux<SegmentEventLog> getEventLogs(ResendSegmentEventDTO body);

    void resendEventLogs(ResendSegmentEventDTO body);

    Mono<Boolean> saveEventLog(SegmentEventLog log);
}
