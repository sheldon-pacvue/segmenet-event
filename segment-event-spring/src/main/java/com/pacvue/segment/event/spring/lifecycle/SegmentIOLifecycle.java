package com.pacvue.segment.event.spring.lifecycle;

import com.pacvue.segment.event.core.SegmentIO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class SegmentIOLifecycle implements SmartLifecycle {
    private boolean running = false;

    @Autowired
    private SegmentIO segmentIO;

    @Override
    public void start() {
        running = true;
        segmentIO.start();
    }


    @Override
    public void stop() {
        segmentIO.tryShutdown();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
