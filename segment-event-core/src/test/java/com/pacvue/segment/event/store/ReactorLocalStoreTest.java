package com.pacvue.segment.event.store;

import com.segment.analytics.messages.Message;
import com.segment.analytics.messages.TrackMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;


class ReactorLocalStoreTest {
    private final static int BUFFER_TIMEOUT = 2;

    private final ReactorLocalStore<Message> reactorLocalStore = ReactorLocalStore.builder().bufferSize(2).bufferTimeoutSeconds(2).build();

    /**
     * shutdown后提交，抛出异常
     */
    @Test
    public void shutdown() {
        reactorLocalStore.accept(System.out::println);
        reactorLocalStore.commit(TrackMessage.builder("11").userId("1").build()).subscribe();
        reactorLocalStore.shutdown();
        Assertions.assertThrows(RuntimeException.class, () ->
                reactorLocalStore.commit(TrackMessage.builder("11").userId("1").build()).subscribe());
    }

    /**
     * 订阅后取消订阅，再次订阅，不会出现重复消费
     * @throws InterruptedException
     */
    @Test
    public void stopAndAcceptAgain() throws InterruptedException, IOException {
        AtomicInteger count = new AtomicInteger();
        StopAccept subscribe = reactorLocalStore.accept((a) -> count.incrementAndGet());
        subscribe.stop();
        reactorLocalStore.accept((a) -> count.incrementAndGet());
        reactorLocalStore.commit(TrackMessage.builder("11").userId("1").build()).subscribe();
        Thread.sleep(BUFFER_TIMEOUT * 1000 + 100);
        Assertions.assertEquals(1, count.get());
    }

    /**
     * 重复订阅抛出异常
     */
    @Test
    public void multiAccept() {
        reactorLocalStore.accept(System.out::println);
        Assertions.assertThrows(IllegalStateException.class, () -> reactorLocalStore.accept(System.out::println));
    }

    @Test
    public void reAccept() throws InterruptedException, IOException {
        AtomicInteger count = new AtomicInteger();
        int c = 0;
        StopAccept retrieve = reactorLocalStore.accept((events) -> {
            for (Message event : events) {
                count.addAndGet(Integer.parseInt(Objects.requireNonNull(event.anonymousId())));
            }
        });
        for (int i = 0; i < 5; i++) {
            reactorLocalStore.commit(TrackMessage.builder(String.valueOf(i)).anonymousId(String.valueOf(i)).build()).subscribe();
            c += i;
        }
        retrieve.stop();
        reactorLocalStore.accept((events) -> {
            for (Message event : events) {
                count.addAndGet(Integer.parseInt(Objects.requireNonNull(event.anonymousId())));
            }
        });
        for (int i = 5; i < 10; i++) {
            reactorLocalStore.commit(TrackMessage.builder(String.valueOf(i)).anonymousId(String.valueOf(i)).build()).subscribe();
            c += i;
        }
        Thread.sleep(BUFFER_TIMEOUT * 1000 + 100);
        Assertions.assertEquals(c, count.get());
    }
}