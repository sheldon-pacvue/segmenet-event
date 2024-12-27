package com.pacvue.segment.event.core;

import com.pacvue.segment.event.holder.ContextHolder;
import com.pacvue.segment.event.store.ReactorLocalStore;
import com.pacvue.segment.event.store.Store;
import lombok.Builder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Optional;

@Builder
public class SegmentIO {
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

    public Mono<Boolean> send(SegmentEventInjector injector) {
        SegmentEvent event = new SegmentEvent();
        if (null != userIdContextHolder) {
            event.setUserId(userIdContextHolder.getContext());
        }
        return injector.inject(event)
                .flatMap(e -> {
                    // 尝试 distributedStore.publish，若失败则尝试 localStore.publish，最后调用 reporter.reportDefault
                    return Optional.ofNullable(distributedStore)
                            .map(store -> store.publish(event)
                                    .onErrorResume(ex -> localStore.publish(event)))
                            .orElse(localStore.publish(event))
                            .onErrorResume(ex -> reporter.reportDefault(List.of(event)));
                });
    }

    private void handleReporter(List<SegmentEvent> events) {
        reporter.reportDefault(events).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @FunctionalInterface
    public interface SegmentEventInjector {
        Mono<SegmentEvent> inject(final SegmentEvent event);
    }
}
