package com.pacvue.segment.event.buffer;

import com.segment.analytics.messages.Message;
import com.segment.analytics.messages.TrackMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


class ReactorLocalBufferTest {
    private final static int BUFFER_TIMEOUT = 2;

    private final Buffer<Message> reactorLocalStore = DefaultBuffer.<Message>builder().build();

    /**
     * shutdown后提交，抛出异常
     */
    @Test
    public void shutdown() throws InterruptedException {
        reactorLocalStore.observer(System.out::println);
        for (int i = 0; i < 14; i++) {
            reactorLocalStore.submit(TrackMessage.builder("123").userId("123").anonymousId("123").build()).subscribe();
        }
        TimeUnit.SECONDS.sleep(6);
    }

    /**
     * 订阅后取消订阅，再次订阅，不会出现重复消费
     * @throws InterruptedException
     */
    @Test
    public void stopAndFlushAgain() throws InterruptedException, IOException {

    }

    /**
     * 重复订阅抛出异常
     */
    @Test
    public void multiFlush() {
    }

    @Test
    public void reFlush() throws InterruptedException, IOException {

    }
}