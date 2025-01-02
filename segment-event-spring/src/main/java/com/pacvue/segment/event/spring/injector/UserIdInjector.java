package com.pacvue.segment.event.spring.injector;

import com.pacvue.segment.event.generator.SegmentEvent;
import com.pacvue.segment.event.holder.ContextHolder;
import reactor.core.publisher.Mono;


public class UserIdInjector implements SegmentEventProcessor {
    private ContextHolder<Integer> userIdHolder;

    @Override
    public Mono<SegmentEvent> doProcess(SegmentEvent event) {
        return Mono.just(event);
    }
}
