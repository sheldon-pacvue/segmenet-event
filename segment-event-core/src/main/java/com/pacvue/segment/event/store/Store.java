package com.pacvue.segment.event.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.segment.analytics.gson.AutoValueAdapterFactory;
import com.segment.analytics.gson.ISO8601DateAdapter;
import com.segment.analytics.messages.Message;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public interface Store<T extends Message>  {
    Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(new AutoValueAdapterFactory())
            .registerTypeAdapter(Date.class, new ISO8601DateAdapter())
            .create();

    // 默认方法，如果没有传入 optional，可以使用默认值
    Mono<Boolean> publish(T event);

    void subscribe(Consumer<List<Message>> consumer, int bundleCount);

    void stopScribe();

    void shutdown();
}
