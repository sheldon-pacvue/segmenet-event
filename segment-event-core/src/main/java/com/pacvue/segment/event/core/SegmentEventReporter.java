package com.pacvue.segment.event.core;

import com.pacvue.segment.event.client.SegmentEventClient;
import com.pacvue.segment.event.client.SegmentEventClientAnalytics;
import com.pacvue.segment.event.client.SegmentEventClientRegistry;
import com.pacvue.segment.event.metric.MetricsCounter;
import com.segment.analytics.messages.Message;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;


@Data
@Builder
public final class SegmentEventReporter {
    @Getter
    private final MetricsCounter metricsCounter;

    @NonNull
    private final SegmentEventClientRegistry registry;

    @Builder.Default
    @Getter
    private Class<? extends SegmentEventClient> defaultClientClass = SegmentEventClientAnalytics.class;

    public Mono<Boolean> report(List<Message> events, Class<? extends SegmentEventClient> clazz) {
        SegmentEventClient client = registry.getClient(clazz);
        return client.send(events)
                .doOnSuccess(b -> {
                    /*
                       事件id是helium10.segmentio.async.send-events
                       内容写入到 @console/runtime/telegraf-metrics.out
                       内容类似 name+methods value timestamp
                       helium10.segmentio.async.send-events.count 100 1245547899
                     */
                    Optional.ofNullable(metricsCounter).ifPresent(counter -> counter.inc(events.size()));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }


    public Mono<Boolean> reportDefault(List<Message> events) {
        return report(events, defaultClientClass);
    }
}
