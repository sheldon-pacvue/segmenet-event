package com.pacvue.segment.event.core;

import com.pacvue.segment.event.entity.*;
import com.pacvue.segment.event.generator.*;
import com.pacvue.segment.event.store.ReactorLocalStore;
import com.pacvue.segment.event.store.Store;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@Builder
public final class SegmentIO {
    private final SegmentEventReporter reporter;
    private final Store<SegmentEvent> distributedStore;
    private final Store<SegmentEventDataBase<SegmentEvent>> dbStore;
    @Builder.Default
    private final Store<SegmentEvent> bufferStore = new ReactorLocalStore<>(10);
    private final int bundleCount = 5;

    public SegmentIO start() {
        if (null != distributedStore) {
            distributedStore.subscribe(list -> list.forEach(bufferStore::publish), bundleCount);
        }
        bufferStore.subscribe(this::handleReporter, bundleCount);
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
                            // 如果分布式存储失败，尝试本地缓冲后直接上报
                            .onErrorResume(ex -> bufferStore.publish(event));
                })
                // IO密集型采用Schedulers.boundedElastic
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void handleReporter(List<SegmentEvent> events) {
        reporter.reportDefault(events)
                // 如果上报成功将记录成功
                .doOnSuccess(b -> {
                    log.debug("consume success, data: {}, result: {}", events, b);
                    Flux.fromIterable(events)
                            .flatMap(event -> {
                                SegmentEventDataBase<SegmentEvent> eventDataBase = new SegmentEventDataBase<>(event, true);
                                return dbStore.publish(eventDataBase)
                                        .doOnError(e -> log.error("event report succeed， but failed to log into db: {}", event, e))
                                        .onErrorResume(e -> Mono.empty());
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();
                })
                // 如果上报失败将存储到数据库中
                .onErrorResume(throwable -> {
                    log.warn("batch report failed, switching to single event processing. Error: {}", throwable.getMessage(), throwable);
                    return Flux.fromIterable(events)
                            .flatMap(event -> {
                                SegmentEventDataBase<SegmentEvent> eventDataBase = new SegmentEventDataBase<>(event, false);
                                return dbStore.publish(eventDataBase)
                                            .doOnError(e -> log.error("failed to log event: {}", event, e))
                                            .onErrorResume(e -> Mono.empty());
                            })
                            .then(Mono.just(Boolean.TRUE)); // 使用 Mono.just(false) 符合返回类型 Mono<Boolean>
                })
                // IO密集型采用Schedulers.boundedElastic
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }
}
