package com.pacvue.segment.event.metric;

public interface MetricsCounter {
    void inc();

    void inc(long amount);
}
