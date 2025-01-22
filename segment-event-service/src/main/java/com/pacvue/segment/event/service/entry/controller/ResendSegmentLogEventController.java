package com.pacvue.segment.event.service.entry.controller;

import com.pacvue.segment.event.service.entity.dto.ResendSegmentEventBody;
import com.pacvue.segment.event.service.service.SegmentEventLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/v1")
public class ResendSegmentLogEventController {
    @Autowired
    private SegmentEventLogService service;

    /**
     * 补发信息
     *
     * @param body 补发范围
     * @return 是否成功
     */
    @PostMapping("/resend")
    public Mono<Boolean> resend(@RequestBody ResendSegmentEventBody body) {
        log.info("resend {}", body);
        // TODO 这里需要写重发逻辑
        service.resendEventLogs(body);
        return Mono.just(Boolean.TRUE);
    }
}
