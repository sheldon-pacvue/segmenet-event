package com.pacvue.segment.event.store;

import org.junit.jupiter.api.Test;


class ReactorLocalStoreTest {

    private final ReactorLocalStore<String> reactorLocalStore = new ReactorLocalStore<>(2);

    @Test
    public void test() throws InterruptedException {
        reactorLocalStore.subscribe(System.out::println, 2);
        for (int i = 0; i < 10; i++) {
            reactorLocalStore.publish("" + i).subscribe();
        }
        reactorLocalStore.publish("123123").subscribe();
        Thread.sleep(4000);
    }
}