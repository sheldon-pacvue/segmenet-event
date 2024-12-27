package com.pacvue.segment.event.core;

import com.pacvue.segment.event.client.SegmentEventClient;
import com.pacvue.segment.event.client.SegmentEventClientHttp;
import com.pacvue.segment.event.client.SegmentEventClientRegistry;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;


@Data
@Builder
public class SegmentEventReporter {
    @NonNull
    private final SegmentEventClientRegistry registry;

    @Builder.Default
    private Class<? extends SegmentEventClient> defaultClientClass = SegmentEventClientHttp.class;

    public Mono<Boolean> report(List<SegmentEvent> events, Class<? extends SegmentEventClient> clazz) {
        SegmentEventClient client = registry.getClient(clazz);
        return client.send(events).subscribeOn(Schedulers.boundedElastic());
    }


    public Mono<Boolean> reportDefault(List<SegmentEvent> events) {
        return report(events, defaultClientClass);
    }
}
