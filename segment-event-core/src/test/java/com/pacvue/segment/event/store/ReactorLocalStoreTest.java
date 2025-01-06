package com.pacvue.segment.event.store;

import com.pacvue.segment.event.entity.SegmentEventTrace;
import org.junit.jupiter.api.Test;


class ReactorLocalStoreTest {

    private final ReactorLocalStore<SegmentEventTrace> reactorLocalStore = new ReactorLocalStore<>(2);

    @Test
    public void test() throws InterruptedException {
        reactorLocalStore.subscribe(System.out::println, 2);
        for (int i = 0; i < 10; i++) {
            SegmentEventTrace segmentEventTrace = new SegmentEventTrace();
            reactorLocalStore.publish(segmentEventTrace).subscribe();
        }
        Thread.sleep(4000);
    }
}