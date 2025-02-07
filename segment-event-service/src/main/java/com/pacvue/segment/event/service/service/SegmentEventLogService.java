package com.pacvue.segment.event.service.service;

import com.mybatis.flex.reactor.core.ReactorService;
import com.pacvue.segment.event.service.entity.dto.ResendSegmentEventDTO;
import com.pacvue.segment.event.service.entity.po.SegmentEventLog;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SegmentEventLogService extends ReactorService<SegmentEventLog> {
    Flux<SegmentEventLog> getEventLogs(ResendSegmentEventDTO body);

    void resendEventLogs(ResendSegmentEventDTO body);

    Mono<Boolean> saveEventLog(SegmentEventLog log);
}
