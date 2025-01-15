package com.pacvue.segment.event.core;

import com.pacvue.segment.event.entity.SegmentLogMessage;
import com.pacvue.segment.event.generator.*;
import com.pacvue.segment.event.metric.MetricsCounter;
import com.pacvue.segment.event.store.ReactorLocalStore;
import com.pacvue.segment.event.store.Store;
import com.segment.analytics.MessageInterceptor;
import com.segment.analytics.MessageTransformer;
import com.segment.analytics.messages.*;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Builder
public final class SegmentIO  {
    private final SegmentEventReporter reporter;
    /**
     * 非必须： 分布式缓冲，用于流量整形，降低峰值负载
     */
    private final Store<Message> distributedStore;
    /**
     * 非必须： report到segment失败，或者记录发送
     */
    private final Store<SegmentLogMessage> logStore;
    /**
     * 必须： 用于本地事件的缓冲，满足条件后批量进行上报
     */
    @Builder.Default
    @NonNull
    private final Store<Message> bufferStore = ReactorLocalStore.builder().bufferSize(5).bufferTimeoutSeconds(10).build().setInstanceId("bufferStore");
    @Builder.Default
    @NonNull
    private final List<MessageTransformer> messageTransformers = new ArrayList<>();
    @Builder.Default
    @NonNull
    private final List<MessageInterceptor> messageInterceptors = new ArrayList<>();
    @NonNull
    private final String secret;
    @NonNull
    private final String reportApp;

    /**
     * 开始接受事件，并且开始上报
     */
    public void start() {
        // 分布式仓库中的数据取出后，存入本地buffer仓库，用于批量上报
        log.info("SegmentIO start");
        Optional.ofNullable(distributedStore).ifPresent(store -> {
            store.accept(list -> list.forEach(bufferStore::commit));
            log.info("SegmentIO distributedStore started");
        });
        // 本地buffer仓库的数据超出阈值后，进行上报
        bufferStore.accept(this::handleReport);
        log.info("SegmentIO bufferStore started");
        log.info("SegmentIO reporter client {} started", reporter.getDefaultClientClass().getSimpleName());
        MetricsCounter metricsCounter = reporter.getMetricsCounter();
        if (null != metricsCounter) {
            log.info("SegmentIO metrics {} started", metricsCounter.getClass().getSimpleName());
        }
    }


    /**
     * 优雅关机，避免spring销毁时，重复调用shutdown方法，这里函数名字不要用close,destroy,shutdown
     */
    public void tryShutdown() {
        // 关闭分布式新事件
        Optional.ofNullable(distributedStore).ifPresent(Store::shutdown);
        // 关闭数据库拉取事件
        Optional.ofNullable(logStore).ifPresent(Store::shutdown);
        // 本地buffer仓库事件清理
        bufferStore.shutdown();
        log.info("SegmentIO shutdown");
    }

    /**
     * 开始从数据库拉取失败事件上报
     */
    public boolean startResend() {
        if (null != logStore) {
            logStore.accept(list -> list.forEach(bufferStore::commit));
            return true;
        }
        return false;
    }


    public void track(SegmentEventGenerator<TrackMessage, TrackMessage.Builder> generator) {
        deliver(generator);
    }

    public void identify(SegmentEventGenerator<IdentifyMessage, IdentifyMessage.Builder> generator) {
        deliver(generator);
    }

    public void group(SegmentEventGenerator<GroupMessage, GroupMessage.Builder> generator) {
        deliver(generator);
    }

    public void page(SegmentEventGenerator<PageMessage, PageMessage.Builder> generator) {
        deliver(generator);
    }

    public void screen(SegmentEventGenerator<ScreenMessage, ScreenMessage.Builder> generator) {
        deliver(generator);
    }

    public <T extends Message, V extends MessageBuilder<T, V>> void deliver(SegmentEventGenerator<T, V> generator) {
        deliverReact(generator).subscribe();
    }

    public <T extends Message, V extends MessageBuilder<T, V>> Mono<Boolean> deliverReact(SegmentEventGenerator<T, V> generator) {
        return generator.generate()
                .mapNotNull(this::buildMessage)
                .flatMap(event -> {
                    // 尝试分布式贮藏
                    return Mono.defer(() -> distributedStore.commit(event))
                            // 如果分布式存储失败，尝试本地缓冲后直接上报
                            .onErrorResume(ex -> bufferStore.commit(event))
                            // 如果本地缓冲失败，则进行持久化
                            .onErrorResume(ex -> tryPersist(event, false));
                })
                .onErrorResume(ex -> {
                    log.error("deliver failed", ex);
                    return Mono.just(Boolean.FALSE);
                })
                // IO密集型采用Schedulers.boundedElastic
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void handleReport(List<Message> events) {
        reporter.reportDefault(events)
                // 如果上报成功将记录成功
                .doOnSuccess(b -> {
                    log.debug("consume success, data: {}, result: {}", events, b);
                    Flux.fromIterable(events)
                            .flatMap(event -> tryPersist(event, true))
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();
                })
                // 如果上报失败，进入持久化仓库
                .onErrorResume(throwable -> {
                    log.warn("batch report failed, switching to single event processing.", throwable);
                    return Flux.fromIterable(events)
                            .flatMap(event -> tryPersist(event, false))
                            .all(result -> result) // 如果所有结果都为 true，返回 true；如果有一个为 false，返回 false
                            .flatMap(Mono::just);
                })
                // IO密集型采用Schedulers.boundedElastic
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private Mono<Boolean> tryPersist(Message event, boolean result) {
        return Mono.defer(() -> logStore.commit(SegmentLogMessage.builder()
                                    .message(event)
                                    .result(result)
                                    .secret(secret)
                                    .reportApp(reportApp)
                .build()))
                .onErrorResume(ex -> {
                    log.warn("persist failed, event: {}", event, ex);
                    return Mono.just(Boolean.FALSE);
                });
    }

    private <V extends MessageBuilder<?, ?>> Message buildMessage(V builder) {
        for (MessageTransformer messageTransformer : messageTransformers) {
            boolean shouldContinue = messageTransformer.transform(builder);
            if (!shouldContinue) {
                log.info("Transformer {} Skipping message {}.", messageTransformer.getClass(), builder);
                return null;
            }
        }
        Message message = builder.build();
        for (MessageInterceptor messageInterceptor : messageInterceptors) {
            message = messageInterceptor.intercept(message);
            if (message == null) {
                log.info("Interceptor {} Skipping message {}.", messageInterceptor.getClass(), builder);
                return null;
            }
        }
        return message;
    }
}
