package com.pacvue.segment.event.store;

import com.segment.analytics.messages.Message;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import reactor.core.Disposable;

import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractStore<T extends Message> implements Store<T> {
    protected Disposable accepted;

    @NotNull
    @Override
    public final StopAccept accept(@NonNull Consumer<List<Message>> consumer) {
        if (isAccepted()) {
            throw new IllegalStateException("Already accepted a consumer.");
        }
        return doAccept(consumer);
    }

    /**
     * 提供子类实现的接受逻辑
     */
    @NonNull
    protected abstract StopAccept doAccept(@NonNull Consumer<List<Message>> consumer);
}