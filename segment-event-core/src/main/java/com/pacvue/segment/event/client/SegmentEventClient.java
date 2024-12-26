package com.pacvue.segment.event.client;

import com.pacvue.segment.event.core.SegmentEvent;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SegmentEventClient {
    Mono<Boolean> send(List<SegmentEvent> event);
}
