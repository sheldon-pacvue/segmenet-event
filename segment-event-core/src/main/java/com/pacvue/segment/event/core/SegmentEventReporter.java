package com.pacvue.segment.event.core;

import com.pacvue.segment.event.client.SegmentEventClient;
import com.pacvue.segment.event.client.SegmentEventClientAnalytics;
import com.pacvue.segment.event.client.SegmentEventClientHttp;
import com.pacvue.segment.event.client.SegmentEventClientRegistry;
import com.pacvue.segment.event.metric.MetricsCounter;
import com.segment.analytics.messages.Message;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;


@Data
@Builder
public final class SegmentEventReporter {
    @NonNull
    private final MetricsCounter metricsCounter;

    @NonNull
    private final SegmentEventClientRegistry registry;

    @Builder.Default
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
                    metricsCounter.inc(events.size());
                });
    }


    public Mono<Boolean> reportDefault(List<Message> events) {
        return report(events, defaultClientClass);
    }
}
