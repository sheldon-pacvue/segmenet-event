package com.pacvue.segment.event.client;

import com.pacvue.segment.event.gson.GsonConstant;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Builder
@Slf4j
public class SegmentEventClientSendReject<T> implements GsonConstant, SegmentEventClient<T> {

    @Override
    public Mono<Boolean> send(T... events) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    }

    @Override
    public void flush() {}
}
