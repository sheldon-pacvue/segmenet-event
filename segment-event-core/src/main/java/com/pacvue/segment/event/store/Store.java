package com.pacvue.segment.event.store;

import com.pacvue.segment.event.gson.Gson;
import com.segment.analytics.messages.Message;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public interface Store<T extends Message> extends Gson {
    /**
     * 存入数据
     */
    @NonNull
    Mono<Boolean> commit(@NonNull T event);

    /**
     * 取出数据
     */
    @NonNull
    StopAccept accept(@NonNull Consumer<List<Message>> consumer);

    /**
     * 优雅关机
     */
    void shutdown();

    /**
     * 是否已经监听的某个accept消费者
     */
    boolean isAccepted();
}
