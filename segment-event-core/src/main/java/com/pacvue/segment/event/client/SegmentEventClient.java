package com.pacvue.segment.event.client;

import com.pacvue.segment.event.gson.Gson;
import com.segment.analytics.messages.Message;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SegmentEventClient extends Gson {
    Mono<Boolean> send(List<Message> events);
}
