package com.pacvue.segment.event.client;

import com.pacvue.segment.event.core.SegmentEvent;
import reactor.core.publisher.Mono;

import java.util.List;

public class SegmentEventClientSocket implements SegmentEventClient {
    @Override
    public Mono<Boolean> send(List<SegmentEvent> events) {
        return null;
    }
}
