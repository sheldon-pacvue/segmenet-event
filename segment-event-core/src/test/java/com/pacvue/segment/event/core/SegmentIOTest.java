package com.pacvue.segment.event.core;

import com.pacvue.segment.event.holder.ContextHolder;
import com.pacvue.segment.event.holder.TtlContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;


@Slf4j
class SegmentIOTest {
    private static final ContextHolder<String> holder = new TtlContextHolder<>();

    @Test
    public void test() throws InterruptedException {
        holder.setContext("123");
        log.info("{}", holder.getContext());
        Flux.range(1, 100).subscribeOn(Schedulers.newParallel("test", 4))
                .subscribe(i -> {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    log.info("??? {}",i + holder.getContext());
                });
        Thread.sleep(10000);
    }
}