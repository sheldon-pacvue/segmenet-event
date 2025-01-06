package com.pacvue.segment.event.store;

import com.pacvue.segment.event.generator.SegmentEvent;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public interface Store<O> {
    // 默认方法，如果没有传入 optional，可以使用默认值
    default Mono<Boolean> publish(SegmentEvent event) {
        return publish(event, null);  // 默认传递 null
    }

    Mono<Boolean> publish(SegmentEvent event, O optional);

    void subscribe(Consumer<List<SegmentEvent>> consumer, int bundleCount);

    void shutdown();
}
