package com.pacvue.segment.event.store;

import com.pacvue.segment.event.entity.SegmentEventTrace;
import org.junit.jupiter.api.Test;


class ReactorLocalStoreTest {

    private final ReactorLocalStore reactorLocalStore = new ReactorLocalStore(2);

    /**
     * 订阅后取消订阅，再次订阅，不会出现重复消费
     * @throws InterruptedException
     */
    @Test
    public void test1() throws InterruptedException {
        reactorLocalStore.subscribe(System.out::println, 2);
        reactorLocalStore.stopScribe();
        reactorLocalStore.subscribe(System.out::println, 2);
        SegmentEventTrace segmentEventTrace = new SegmentEventTrace();
        segmentEventTrace.setUserId("1");
        reactorLocalStore.publish(segmentEventTrace).subscribe();
        Thread.sleep(4000);
    }

    /**
     * 多次订阅，不会出现重复消费
     * @throws InterruptedException
     */
    @Test
    public void test2() throws InterruptedException {
        reactorLocalStore.subscribe(System.out::println, 2);
        reactorLocalStore.subscribe(System.out::println, 2);
        SegmentEventTrace segmentEventTrace = new SegmentEventTrace();
        segmentEventTrace.setUserId("1");
        reactorLocalStore.publish(segmentEventTrace).subscribe();
        Thread.sleep(4000);
    }

    @Test
    public void test20() throws InterruptedException {
        reactorLocalStore.subscribe(System.out::println, 2);
        for (int i = 0; i < 5; i++) {
            SegmentEventTrace segmentEventTrace = new SegmentEventTrace();
            reactorLocalStore.publish(segmentEventTrace).subscribe();
        }
        reactorLocalStore.stopScribe();
        reactorLocalStore.subscribe(System.out::println, 2);
        for (int i = 5; i < 10; i++) {
            SegmentEventTrace segmentEventTrace = new SegmentEventTrace();
            reactorLocalStore.publish(segmentEventTrace).subscribe();
        }
        Thread.sleep(4000);
    }
}