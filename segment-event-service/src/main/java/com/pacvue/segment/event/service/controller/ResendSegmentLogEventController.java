package com.pacvue.segment.event.service.controller;

import com.pacvue.segment.event.service.entity.dto.Batch;
import com.pacvue.segment.event.service.entity.dto.ResendSegmentEventBody;
import com.pacvue.segment.event.service.entity.dto.TrackBody;
import com.segment.analytics.messages.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/v1")
public class ResendSegmentLogEventController {

    /**
     * 仅补发未发送成功的日志
     *
     * @param body 补发范围
     * @return 是否成功
     */
    @PostMapping("/resend")
    public Mono<Boolean> resend(@RequestBody ResendSegmentEventBody body) {
        log.info("resend {}", body);
        // TODO 这里需要写重发逻辑

        return Mono.just(Boolean.TRUE);
    }

    /**
     * 强制发送日志
     *
     * @param body 补发范围
     * @return 是否成功
     */
    @PostMapping("/resend/focus")
    public Mono<Boolean> resendFocus(@RequestBody ResendSegmentEventBody body) {
        log.info("resend focus {}", body);
        // TODO 这里需要写重发逻辑

        return Mono.just(Boolean.TRUE);
    }
}
