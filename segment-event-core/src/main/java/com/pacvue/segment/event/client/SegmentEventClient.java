package com.pacvue.segment.event.client;

import com.segment.analytics.messages.Message;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SegmentEventClient {
    Mono<Boolean> send(List<Message> events);
}
