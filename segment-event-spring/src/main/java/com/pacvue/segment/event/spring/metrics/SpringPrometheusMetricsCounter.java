package com.pacvue.segment.event.spring.metrics;

import com.pacvue.segment.event.metric.MetricsCounter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

public class SpringPrometheusMetricsCounter implements MetricsCounter {
    private final Counter counter;

    private SpringPrometheusMetricsCounter(Counter counter) {
        this.counter = counter;
    }

    @Override
    public void inc() {
        counter.increment();
    }

    @Override
    public void inc(long amount) {
        counter.increment(amount);
    }

    public static Builder builder(MeterRegistry registry, String name) {
        return new Builder(registry, name);
    }

    @Data
    @Accessors(fluent = true, chain = true)
    @RequiredArgsConstructor
    public static class Builder {
        private final MeterRegistry registry;
        private final String name;
        private String[] tags;

        public SpringPrometheusMetricsCounter build() {
            return new SpringPrometheusMetricsCounter(registry.counter(name, tags));
        }
    }
}
