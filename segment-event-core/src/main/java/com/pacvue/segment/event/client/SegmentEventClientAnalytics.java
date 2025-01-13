package com.pacvue.segment.event.client;

import com.segment.analytics.Analytics;
import com.segment.analytics.internal.AnalyticsClient;
import com.segment.analytics.messages.Message;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.List;

@Slf4j
public class SegmentEventClientAnalytics implements SegmentEventClient {
    private final AnalyticsClient client;

    SegmentEventClientAnalytics(Analytics analytics) throws NoSuchFieldException, IllegalAccessException {
        Field field = Analytics.class.getDeclaredField("client");
        field.setAccessible(true); // 允许访问私有字段
        this.client = (AnalyticsClient) field.get(analytics);
    }

    @Override
    public Mono<Boolean> send(List<Message> events) {
        return Mono.defer(() -> {
            for (Message event : events) {
                client.enqueue(event);
            }
            return Mono.just(true);
        });
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Analytics analytics;

        public Builder analytics(Analytics analytics) {
            this.analytics = analytics;
            return this;
        }

        public SegmentEventClientAnalytics build() throws NoSuchFieldException, IllegalAccessException {
            return new SegmentEventClientAnalytics(analytics);
        }
    }
}
