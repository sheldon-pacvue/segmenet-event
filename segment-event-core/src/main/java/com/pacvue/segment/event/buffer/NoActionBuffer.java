package com.pacvue.segment.event.buffer;

import com.segment.analytics.messages.Message;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Builder
@Slf4j
public class NoActionBuffer<T extends Message> implements Buffer<T> {
    @Override
    public @NonNull Mono<Boolean> submit(@NonNull T event) {
        throw new RuntimeException("don't allow submit");
    }

    @Override
    public void flush() {
        throw new RuntimeException("don't allow flush");
    }

    @Override
    public StopObserver observer(@NotNull Function<List<T>, Mono<Boolean>> observer) {
        throw new RuntimeException("don't allow observer");
    }

    @Override
    public void shutdown() {

    }
}
