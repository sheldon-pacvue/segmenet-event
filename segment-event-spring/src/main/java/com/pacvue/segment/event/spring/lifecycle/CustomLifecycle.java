package com.pacvue.segment.event.spring.lifecycle;

import com.pacvue.segment.event.core.SegmentIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomLifecycle implements SmartLifecycle {
    private boolean running = false;
    @Autowired
    private SegmentIO segmentIO;

    @Override
    public void start() {
        running = true;
        segmentIO.start();
        log.info("SegmentIO started...");
    }

    @Override
    public void stop() {
        // 自定义停机逻辑
        segmentIO.shutdown();
        log.info("SegmentIO stopped...");
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }
}