package com.pacvue.segment.event.client;

public interface SegmentEventClientRegistry {
    <T extends SegmentEventClient> T getClient(Class<T> tClass);
}
