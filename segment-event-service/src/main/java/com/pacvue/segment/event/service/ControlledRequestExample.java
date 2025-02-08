package com.pacvue.segment.event.service;

import com.pacvue.segment.event.service.entity.dto.SegmentEventLogCursor;
import com.pacvue.segment.event.service.entity.po.SegmentEventLog;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ControlledRequestExample {

    public static void main(String[] args) throws InterruptedException {
        Flux<Integer> flux = Flux.<Integer>create(emitter -> {
            AtomicInteger integer = new AtomicInteger(0);
            do {
                // 获取下游请求的数据量
                long requested = emitter.requestedFromDownstream();

                // 如果 requested == 0，说明消费端繁忙，等待一段时间
                while (requested == 0) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(100); // 100ms 休眠，避免 CPU 过高
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        emitter.complete();
                        return;
                    }
                    requested = emitter.requestedFromDownstream(); // 重新获取请求量
                }
                while (requested-- > 0) {
                    emitter.next(integer.incrementAndGet());
                }
            } while (true);
        }).subscribeOn(Schedulers.boundedElastic());

        flux.subscribe(new BaseSubscriber<>() {
            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                // 初始请求 1 个数据
                request(1);
            }

            @Override
            protected void hookOnNext(Integer value) {
                // 处理数据
                System.out.println("Processed: " + value);

                // 模拟慢速消费
                try {
                    Thread.sleep(100); // 每 500ms 处理一个数据
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // 每次处理完一个数据后，再请求 1 个数据
                request(1);
            }
        });

        Thread.sleep(10_000_000);
    }
}