package com.pacvue.segment.event.core;

import com.pacvue.segment.event.store.ReactorLocalStore;
import com.pacvue.segment.event.store.Store;
import lombok.Builder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Builder
public class SegmentIO {
    private final SegmentEventReporter reporter;
    private final Store<SegmentEvent> distributedStore;
    private final Store<SegmentEvent> localStore = new ReactorLocalStore();
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
        return injector.inject(event)
                .flatMap(distributedStore::publish)
                .onErrorResume(e -> localStore.publish(event))
                .onErrorResume(e -> reporter.reportDefault(List.of(event)));
    }

    private void handleReporter(List<SegmentEvent> events) {
        reporter.reportDefault(events).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @FunctionalInterface
    public interface SegmentEventInjector {
        Mono<SegmentEvent> inject(final SegmentEvent event);
    }
}
