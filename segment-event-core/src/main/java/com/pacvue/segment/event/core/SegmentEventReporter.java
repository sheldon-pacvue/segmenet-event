package com.pacvue.segment.event.core;

import com.pacvue.segment.event.client.SegmentEventClient;
import com.pacvue.segment.event.client.SegmentEventClientHttp;
import com.pacvue.segment.event.client.SegmentEventClientRegistry;
import com.pacvue.segment.event.generator.SegmentEvent;
import com.pacvue.segment.event.metric.MetricsCounter;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang3.concurrent.ThresholdCircuitBreaker;
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
    private Class<? extends SegmentEventClient> defaultClientClass = SegmentEventClientHttp.class;

    public Mono<Boolean> report(List<SegmentEvent> events, Class<? extends SegmentEventClient> clazz) {
        SegmentEventClient client = registry.getClient(clazz);
        return client.send(events)
                .doOnSuccess(b -> {
                    /**
                     *  TODO 这里需要添加计数器Metric
                     *  事件id是helium10.segmentio.async.send-events
                     *  内容写入到 @console/runtime/telegraf-metrics.out
                     *  内容类似 name+methods value timestamp
                     *  helium10.segmentio.async.send-events.count 100 1245547899
                     */
                    metricsCounter.inc(events.size());
                });
    }


    public Mono<Boolean> reportDefault(List<SegmentEvent> events) {
        return report(events, defaultClientClass);
    }
}
