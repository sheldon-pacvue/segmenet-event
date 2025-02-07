package com.pacvue.segment.event.service.entry.controller;

import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.entity.SegmentEventLogMessage;
import com.pacvue.segment.event.service.entity.po.SegmentEventLog;
import com.pacvue.segment.event.service.service.SegmentEventLogService;
import com.segment.analytics.messages.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/v1")
public class SegmentEventController {
    @Autowired
    private SegmentEventLogService service;

    @Autowired
    private SegmentIO segmentIO;

    @PostMapping("/report/event")
    public Mono<Boolean> report(@RequestBody Message message) {
        log.info("{}", message);
        segmentIO.message(message);
        return Mono.just(Boolean.TRUE);
    }

    @PostMapping("/report/log")
    public Mono<Boolean> report(@RequestBody SegmentEventLogMessage message) {
        log.info("{}", message);
        return service.saveEventLog(new SegmentEventLog().covert(message));
    }
}
