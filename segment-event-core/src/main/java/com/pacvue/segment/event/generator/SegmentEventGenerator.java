package com.pacvue.segment.event.generator;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface SegmentEventGenerator<T extends SegmentEvent>  {
    Mono<T> generate(Class<T> clazz);
}
