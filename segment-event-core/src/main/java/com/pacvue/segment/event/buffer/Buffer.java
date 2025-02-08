package com.pacvue.segment.event.buffer;

import com.pacvue.segment.event.gson.GsonConstant;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Buffer<T> extends GsonConstant {
    /**
     * 存入数据
     */
    @NonNull
    Mono<Boolean> submit(@NonNull T event);

    /**
     * 冲刷数据
     */
    void flush();


    /**
     * 订阅数据
     */
    StopObserver observer(@NotNull Function<List<T>, Mono<Boolean>> observer);

    /**
     * 优雅关机
     */
    void shutdown();
}
