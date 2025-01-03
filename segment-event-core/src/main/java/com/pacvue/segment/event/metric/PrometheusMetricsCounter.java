package com.pacvue.segment.event.metric;

import io.prometheus.client.Counter;
import io.prometheus.client.exporter.HTTPServer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * 非spring框架才需要使用，会暴露7994端口，通过/metrics端点进行访问
 */
public class PrometheusMetricsCounter implements MetricsCounter {
    private final Counter requestCounter;
    private final Supplier<String[]> labelValuesSupplier;
    private final HTTPServer httpServer;

    private PrometheusMetricsCounter(Counter requestCounter, Supplier<String[]> supplier) {
        this.requestCounter = requestCounter;
        this.labelValuesSupplier = supplier;
        try {
            this.httpServer = new HTTPServer(7994);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public void close() {
        this.httpServer.close();
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
