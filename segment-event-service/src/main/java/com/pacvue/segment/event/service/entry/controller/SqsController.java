package com.pacvue.segment.event.service.entry.controller;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/v1")
public class SqsController {
    @Autowired
    private SqsTemplate sqsTemplate;


    @PostMapping("/sqs")
    public Mono<Boolean> sqs() {
        sqsTemplate.send(sqsSendOptions -> sqsSendOptions.queue("helium10-segment-dev").payload("123"));
        return Mono.just(Boolean.TRUE);
    }
}
