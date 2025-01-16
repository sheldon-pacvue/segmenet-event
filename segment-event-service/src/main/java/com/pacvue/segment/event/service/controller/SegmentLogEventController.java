package com.pacvue.segment.event.service.controller;

import com.pacvue.segment.event.entity.SegmentLogMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/v1/segment/log/event")
public class SegmentLogEventController {
    @PostMapping("/report")
    public Mono<Boolean> report(@RequestBody SegmentLogMessage message) {
        log.info("{}", message);

        return Mono.just(Boolean.TRUE);
    }
}
