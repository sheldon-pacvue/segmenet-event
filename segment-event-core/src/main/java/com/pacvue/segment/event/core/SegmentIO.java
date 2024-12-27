package com.pacvue.segment.event.core;

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
    private final Store<SegmentEvent> localStore = new ReactorLocalStore<>(10);
    private ContextHolder<Integer> userIdContextHolder;
    private final int bundleCount = 5;

    public SegmentIO startSubscribe() {
        if (null != distributedStore) {
            distributedStore.subscribe(this::handleReporter, bundleCount);
        }
        localStore.subscribe(this::handleReporter, bundleCount);
        return this;
    }

    public void deliver(SegmentEventInjector injector) {
        deliverReact(injector).subscribe();
    }

    public Mono<Boolean> deliverReact(SegmentEventInjector injector) {
        SegmentEvent event = new SegmentEvent();
        if (null != userIdContextHolder) {
            event.setUserId(userIdContextHolder.getContext());
        }
        return injector.inject(event)
                .flatMap(e -> distributedStore.publish(event))
                .onErrorResume(ex -> localStore.publish(event))
                .onErrorResume(ex -> reporter.reportDefault(List.of(event)))
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

    @FunctionalInterface
    public interface SegmentEventInjector {
        Mono<SegmentEvent> inject(final SegmentEvent event);
    }
}
