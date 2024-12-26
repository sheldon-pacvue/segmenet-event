package com.pacvue.segment.event.holder;

import com.alibaba.ttl.TransmittableThreadLocal;

public class TtlContextHolder<T> implements ContextHolder<T> {
    protected final TransmittableThreadLocal<T> ttl = new TransmittableThreadLocal<>();

    @Override
    public T getContext() {
        return ttl.get();
    }

    @Override
    public void setContext(T context) {
        ttl.set(context);
    }
}
