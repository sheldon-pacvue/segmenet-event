package com.pacvue.segment.event.core;

import com.pacvue.segment.event.entity.*;
import com.pacvue.segment.event.generator.*;
import com.pacvue.segment.event.holder.ContextHolder;
import com.pacvue.segment.event.store.ReactorLocalStore;
import com.pacvue.segment.event.store.Store;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@Builder
public final class SegmentIO {
    private final SegmentEventReporter reporter;
    private final Store<SegmentEvent> distributedStore;
    @Builder.Default
    private final Store<SegmentEvent> localStore = new ReactorLocalStore<>(10);
    private final int bundleCount = 5;

    public SegmentIO start() {
        if (null != distributedStore) {
            distributedStore.subscribe(list -> list.forEach(localStore::publish), bundleCount);
        }
        localStore.subscribe(this::handleReporter, bundleCount);
        return this;
    }

    public void trace(SegmentEventGenerator<SegmentEventTrace> generator) {
        deliver(generator, SegmentEventTrace.class);
    }

    public void identify(SegmentEventGenerator<SegmentEventIdentify> generator) {
        deliver(generator, SegmentEventIdentify.class);
    }

    public void group(SegmentEventGenerator<SegmentEventGroup> generator) {
        deliver(generator, SegmentEventGroup.class);
    }

    public void page(SegmentEventGenerator<SegmentEventPage> generator) {
        deliver(generator, SegmentEventPage.class);
    }

    public void screen(SegmentEventGenerator<SegmentEventScreen> generator) {
        deliver(generator, SegmentEventScreen.class);
    }

    public <T extends SegmentEvent> void deliver(SegmentEventGenerator<T> generator, Class<T> clazz) {
        deliverReact(generator, clazz).subscribe();
    }

    public <T extends SegmentEvent> Mono<Boolean> deliverReact(SegmentEventGenerator<T> generator, Class<T> clazz) {
        return generator.generate(clazz)
                .flatMap(event -> {
                    // 使用 Mono.defer 来延迟执行
                    // 尝试分布式贮藏
                    return Mono.defer(() -> distributedStore.publish(event))
                            // 如果分布式存储失败，尝试本地存储
                            .onErrorResume(ex -> localStore.publish(event))
                            // 如果本地存储失败，尝试默认报告
                            .onErrorResume(ex -> reporter.reportDefault(List.of(event)));
                })
                // IO密集型采用Schedulers.boundedElastic
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void handleReporter(List<SegmentEvent> events) {
        reporter.reportDefault(events)
                .doOnSuccess(b -> log.debug("consume success, data: {}, result: {}", events, b))
                .onErrorResume(throwable -> {
                    log.error("consume failed, data: {}", events, throwable);
                    return Mono.empty();
                })
                // IO密集型采用Schedulers.boundedElastic
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }
}
