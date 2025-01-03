package com.pacvue.segment.event.metric;

import io.prometheus.client.Counter;
import io.prometheus.client.Supplier;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

public class PrometheusMetricsCounter implements MetricsCounter {
    private final Counter requestCounter;
    private final Supplier<String[]> labelValuesSupplier;

    private PrometheusMetricsCounter(Counter requestCounter, Supplier<String[]> supplier) {
        this.requestCounter = requestCounter;
        this.labelValuesSupplier = supplier;
    }

    @Override
    public void inc() {
        if (labelValuesSupplier != null) {
            requestCounter.labels(labelValuesSupplier.get());
        }
        requestCounter.inc();
    }

    @Override
    public void inc(long amount) {
        if (labelValuesSupplier != null) {
            requestCounter.labels(labelValuesSupplier.get());
        }
        requestCounter.inc(amount);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    @Data
    @Accessors(fluent = true, chain = true)
    @RequiredArgsConstructor
    public static class Builder {
        private final String name;
        private String subSystem;
        private String namespace;
        private String help;
        private String[] labelNames;
        private Supplier<String[]> labelValuesSupplier;

        public PrometheusMetricsCounter build() {
            return new PrometheusMetricsCounter(Counter.build().name(name).subsystem(subSystem).namespace(namespace).help(help).labelNames(labelNames).register(), labelValuesSupplier);
        }
    }
}
