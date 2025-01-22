package com.pacvue.segment.event.buffer;

import com.pacvue.segment.event.gson.GsonConstant;
import com.segment.analytics.messages.Message;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public interface Buffer<T> extends GsonConstant {
    /**
     * 存入数据
     */
    @NonNull
    Mono<Boolean> commit(@NonNull T event);

    /**
     * 取出数据
     */
    @NonNull
    StopAccept accept(@NonNull Consumer<List<T>> consumer);

    /**
     * 优雅关机
     */
    void shutdown();
}
