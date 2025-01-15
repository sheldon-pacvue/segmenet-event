package com.pacvue.segment.event.buffer;

import com.segment.analytics.messages.Message;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

@Builder
@Slf4j
public class NoActionBuffer<T extends Message> extends AbstractBuffer<T> {

    @NotNull
    @Override
    public Mono<Boolean> commit(@NotNull T event) {
        throw new RuntimeException("don't allow commit");
    }

    @NotNull
    @Override
    protected StopAccept doAccept(@NotNull Consumer<List<Message>> consumer) {
       throw new RuntimeException("don't allow accept");
    }

    @Override
    public void shutdown() {

    }
}
