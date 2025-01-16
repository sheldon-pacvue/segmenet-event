package com.pacvue.segment.event.client;

import com.pacvue.segment.event.gson.GsonConstant;
import com.segment.analytics.messages.Message;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SegmentEventClient<T extends Message> extends GsonConstant {
    Mono<Boolean> send(List<T> events);
}
