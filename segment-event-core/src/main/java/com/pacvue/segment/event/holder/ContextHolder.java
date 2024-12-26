package com.pacvue.segment.event.holder;

public interface ContextHolder<T> {
    T getContext();

    void setContext(T context);
}
