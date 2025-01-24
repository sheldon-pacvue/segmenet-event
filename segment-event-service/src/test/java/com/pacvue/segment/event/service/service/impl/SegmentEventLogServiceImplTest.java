package com.pacvue.segment.event.service.service.impl;

import com.pacvue.segment.event.service.entity.dto.ResendSegmentEventDTO;
import com.pacvue.segment.event.service.service.SegmentEventLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

@SpringBootTest
class SegmentEventLogServiceImplTest {
    @Autowired
    private SegmentEventLogService service;

    @Test
    void getEventLogs() {
        service.getEventLogs(new ResendSegmentEventDTO())
                .doOnNext(System.out::println)
                .blockLast();
    }

    @Test
    void getUnsentEventLogs() {
        service.resendEventLogs(new ResendSegmentEventDTO());
        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}