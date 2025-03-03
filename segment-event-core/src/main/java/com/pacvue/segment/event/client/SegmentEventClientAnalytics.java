package com.pacvue.segment.event.client;

import com.segment.analytics.Analytics;
import com.segment.analytics.internal.AnalyticsClient;
import com.segment.analytics.messages.Message;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SegmentEventClientAnalytics<T extends Message> implements SegmentEventClient<T> {
    private final AnalyticsClient client;

    SegmentEventClientAnalytics(Analytics analytics) throws NoSuchFieldException, IllegalAccessException {
        Field field = Analytics.class.getDeclaredField("client");
        field.setAccessible(true); // 允许访问私有字段
        this.client = (AnalyticsClient) field.get(analytics);
    }

    @Override
    public Mono<Boolean> send(T... events) {
        return Flux.fromArray(events)
                .flatMap(event -> {
                    client.enqueue(event);
                    return Mono.just(Boolean.TRUE);
                })
                .all(success -> success)
                .defaultIfEmpty(Boolean.TRUE);
    }

    @Override
    public void flush() {
        client.shutdown();
    }

    public static <T extends Message> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T extends Message> {
        private Analytics analytics;

        public Builder<T> analytics(Analytics analytics) {
            this.analytics = analytics;
            return this;
        }

        public SegmentEventClientAnalytics<T> build() throws NoSuchFieldException, IllegalAccessException {
            return new SegmentEventClientAnalytics<>(analytics);
        }
    }
}
