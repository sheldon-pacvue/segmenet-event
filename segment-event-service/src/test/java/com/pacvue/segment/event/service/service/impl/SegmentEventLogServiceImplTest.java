package com.pacvue.segment.event.service.service.impl;

import com.pacvue.segment.event.service.entity.dto.ResendSegmentEventDTO;
import com.pacvue.segment.event.service.service.SegmentEventLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
class SegmentEventLogServiceImplTest {
    @Autowired
    private SegmentEventLogService service;

    @Test
    void getEventLogs() throws InterruptedException {
        ResendSegmentEventDTO resendSegmentEventDTO = new ResendSegmentEventDTO();
        AtomicInteger i = new AtomicInteger(0);
        service.getEventLogs(resendSegmentEventDTO)
                .subscribe(data -> System.out.println("count: " + i.addAndGet(1)));
        Thread.sleep(100_000);
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