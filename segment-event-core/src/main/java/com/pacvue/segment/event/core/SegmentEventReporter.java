package com.pacvue.segment.event.core;

import com.pacvue.segment.event.client.SegmentEventClient;
import com.pacvue.segment.event.client.SegmentEventClientHttp;
import com.pacvue.segment.event.client.SegmentEventClientRegistry;
import com.pacvue.segment.event.generator.SegmentEvent;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;


@Data
@Builder
public final class SegmentEventReporter {
    @NonNull
    private final SegmentEventClientRegistry registry;

    @Builder.Default
    private Class<? extends SegmentEventClient> defaultClientClass = SegmentEventClientHttp.class;

    public Mono<Boolean> report(List<SegmentEvent> events, Class<? extends SegmentEventClient> clazz) {
        SegmentEventClient client = registry.getClient(clazz);
        return client.send(events);
    }


    public Mono<Boolean> reportDefault(List<SegmentEvent> events) {
        return report(events, defaultClientClass);
    }
}
