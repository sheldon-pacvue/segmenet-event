package com.pacvue.segment.event.spring.lifecycle;

import com.pacvue.segment.event.core.SegmentIO;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class SegmentIOLifecycle implements SmartLifecycle, ApplicationContextAware {
    private boolean running = false;
    private ApplicationContext applicationContext;

    @Override
    public void start() {
        running = true;
        try {
            SegmentIO bean = applicationContext.getBean(SegmentIO.class);
            bean.start();
        } catch (Exception ignore) {}
    }


    @Override
    public void stop() {
        try {
            SegmentIO bean = applicationContext.getBean(SegmentIO.class);
            bean.tryShutdown();
        } catch (Exception ignore) {}
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
