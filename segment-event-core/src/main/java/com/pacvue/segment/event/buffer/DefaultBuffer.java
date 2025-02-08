package com.pacvue.segment.event.buffer;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class DefaultBuffer<T> implements Buffer<T> {
    private final int maxSize; // 缓冲区最大大小
    private final long flushInterval; // 刷新间隔时间（毫秒）
    private final List<T> buffer; // 缓冲区
    private final List<Function<List<T>, Mono<Boolean>>> observers; // 观察者列表
    private final ScheduledExecutorService scheduler; // 定时任务调度器

    DefaultBuffer(int maxSize, long flushInterval) {
        this.maxSize = maxSize;
        this.flushInterval = flushInterval;
        this.buffer = new ArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        // 启动定时刷新任务
        scheduleFlushTask();
    }

    @NotNull
    @Override
    public Mono<Boolean> submit(@NonNull T event) {
        return Mono.fromCallable(() -> {
            synchronized (buffer) {
                buffer.add(event);
                if (buffer.size() >= maxSize) {
                    flush();
                }
            }
            return true;
        }).subscribeOn(Schedulers.fromExecutor(this.scheduler));
    }

    @Override
    public void flush() {
        synchronized (buffer) {
            if (!buffer.isEmpty()) {
                List<T> dataToFlush = new ArrayList<>(buffer);
                buffer.clear();
                // 这里需要等待消费者都消费结束
                notifyObservers(dataToFlush).block(); // 通知所有观察者
            }
        }
    }

    @Override
    public StopObserver observer(@NotNull Function<List<T>, Mono<Boolean>> observer) {
        observers.add(observer);
        return () -> observers.remove(observer); // 返回一个取消订阅的函数
    }

    @Override
    public void shutdown() {
        scheduler.shutdown(); // 关闭调度器
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow(); // 强制关闭
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        flush(); // 关机前刷新剩余数据
    }

    private void scheduleFlushTask() {
        scheduler.scheduleAtFixedRate(this::flush, flushInterval, flushInterval, TimeUnit.MILLISECONDS);
    }

    private Mono<Boolean> notifyObservers(List<T> data) {
        return Flux.fromIterable(observers)
                .flatMap(observer -> {
                    return observer.apply(data);
                })
                .all(success -> success)
                .subscribeOn(Schedulers.boundedElastic());
    }


    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private int bufferSize = 10;
        private int flushInterval = 5_000;

        public Builder<T> bufferSize(int bufferSize) {
            if (bufferSize > 0) {
                this.bufferSize = bufferSize;
            }
            return this;
        }

        public Builder<T> flushInterval(int flushInterval) {
            if (flushInterval > 0) {
                this.flushInterval = flushInterval;
            }
            return this;
        }

        public Buffer<T> build() {
            return new DefaultBuffer<>(bufferSize, flushInterval);
        }
    }
}