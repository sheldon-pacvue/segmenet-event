package com.pacvue.segment.event.service.controller;

import com.pacvue.segment.event.service.entity.dto.Batch;
import com.segment.analytics.messages.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/v1")
public class SegmentEventController {
    @PostMapping("/report")
    public Mono<Boolean> report(@RequestBody Message message) {
        log.info("{}", message);

        return Mono.just(Boolean.TRUE);
    }

    @PostMapping("/batch/report")
    public Mono<Boolean> batch(@RequestBody Batch message) {
        log.info("{}", message);

        return Mono.just(Boolean.TRUE);
    }
}
