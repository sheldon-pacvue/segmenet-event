package com.pacvue.segment.event.service.entry.controller;

import com.pacvue.segment.event.gson.GsonConstant;
import com.segment.analytics.messages.Message;
import com.segment.analytics.messages.TrackMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/v1")
public class SqsController implements GsonConstant {
    @Autowired
    private SqsTemplate sqsTemplate;


    @PostMapping("/sqs")
    public Mono<Boolean> sqs(@RequestBody Message message) {
        sqsTemplate.send(sqsSendOptions -> sqsSendOptions.queue("helium10-segment-dev")
                .header(MessageHeaders.CONTENT_TYPE, "application/json")
                .payload(TrackMessage.builder("message-event")
                        .properties(message.integrations())
                        .anonymousId(message.anonymousId())
                        .userId(message.userId())
                        .sentAt(message.sentAt())
                        .build()));
        return Mono.just(Boolean.TRUE);
    }
}
