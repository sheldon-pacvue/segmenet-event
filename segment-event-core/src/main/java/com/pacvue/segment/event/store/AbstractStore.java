package com.pacvue.segment.event.store;

import com.segment.analytics.messages.Message;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Accessors(chain = true)
public abstract class AbstractStore<T extends Message> implements Store<T> {
    @Setter
    protected String instanceId = UUID.randomUUID().toString();
    protected boolean isAccepted = false;

    @NotNull
    @Override
    public final StopAccept accept(@NonNull Consumer<List<Message>> consumer) {
        if (isAccepted) {
            throw new IllegalStateException("Already accepted a consumer.");
        }
        StopAccept stopAccept = doAccept(consumer);
        this.isAccepted = true;
        return () -> {
            stopAccept.stop();
            this.isAccepted = false;
        };
    }

    /**
     * 提供子类实现的接受逻辑
     */
    @NonNull
    protected abstract StopAccept doAccept(@NonNull Consumer<List<Message>> consumer);
}