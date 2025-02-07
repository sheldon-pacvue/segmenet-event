package com.pacvue.segment.event.service;

import com.pacvue.segment.event.core.SegmentIO;
import com.segment.analytics.messages.TrackMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@SpringBootTest
class ReactorLocalBufferTest {
    @Autowired
    private SegmentIO segmentIO;

    @Test
    public void test() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            segmentIO.deliverReact(TrackMessage.builder("123")
                    .userId("12321")
                    .anonymousId("1232333")
                    .sentAt(new Date())
                    .build()).subscribe();
        }

        Thread.sleep(100_000);
    }
}
