package com.pacvue.segment.event.client;

import com.pacvue.segment.event.generator.SegmentEvent;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SegmentEventClient {
    Mono<Boolean> send(List<SegmentEvent> event);
}
