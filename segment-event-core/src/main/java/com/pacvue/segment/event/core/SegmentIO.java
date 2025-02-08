package com.pacvue.segment.event.core;

import com.pacvue.segment.event.entity.MessageLog;
import com.pacvue.segment.event.generator.*;
import com.pacvue.segment.event.gson.GsonConstant;
import com.pacvue.segment.event.metric.MetricsCounter;
import com.pacvue.segment.event.extend.ReactorMessageInterceptor;
import com.pacvue.segment.event.extend.ReactorMessageTransformer;
import com.segment.analytics.messages.*;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Builder
public final class SegmentIO implements GsonConstant {
    private final SegmentEventReporter<? extends MessageLog<?>> reporter;
    @Builder.Default
    @NonNull
    private final List<ReactorMessageTransformer> messageTransformers = new ArrayList<>();
    @Builder.Default
    @NonNull
    private final List<ReactorMessageInterceptor> messageInterceptors = new ArrayList<>();

    /**
     * 开始接受事件，并且开始上报
     */
    public void start() {
        // 分布式仓库中的数据取出后，存入本地buffer仓库，用于批量上报
        log.info("SegmentIO start");
        reporter.init();
        MetricsCounter metricsCounter = reporter.getMetricsCounter();
        if (null != metricsCounter) {
            log.info("SegmentIO metrics {} started", metricsCounter.getClass().getSimpleName());
        }
    }

    /**
     * 优雅关机，避免spring销毁时，重复调用shutdown方法，这里函数名字不要用close,destroy,shutdown
     */
    public void tryShutdown() {
        reporter.flush();
        log.info("SegmentIO shutdown");
    }

    public void message(String message) {
        deliver(interceptMessage(gson.fromJson(message, Message.class)));
    }

    public void message(Message message) {
        deliver(interceptMessage(message));
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

    private <T extends Message> void deliver(Mono<T> message) {
        deliverReact(message).subscribe();
    }

    public <T extends Message, V extends MessageBuilder<T, V>> Mono<Boolean> deliverReact(SegmentEventGenerator<T, V> generator) {
        return deliverReact(generator.generate().flatMap(this::interceptMessage));
    }

    public <T extends Message> Mono<Boolean> deliverReact(Mono<T> message) {
        return message.flatMap(this::deliverReact);
    }

    public <T extends Message> void deliverReact(Flux<T> messages) {
        messages.subscribe(new BaseSubscriber<T>() {
            @Override
            protected void hookOnSubscribe(@NotNull Subscription subscription) {
                request(1);
            }

            @Override
            protected void hookOnNext(@NotNull T message) {
                log.info("message: {}", message);
                deliverReact(message).block();
                request(1);
            }
        });
    }

    public <T extends Message> Mono<Boolean> deliverReact(T message) {
        return reporter.report(message).subscribeOn(Schedulers.boundedElastic());
    }

    private <V extends MessageBuilder<?, ?>> Mono<Message> interceptMessage(V builder) {
        return Mono.deferContextual(ctx -> Flux.fromIterable(messageTransformers)
                .flatMap(transformer -> transformer.transform(builder, ctx)
                        .onErrorResume(ex -> {
                            log.error("Transformer {} failed, skipping message {}.", transformer.getClass(), builder, ex);
                            return Mono.just(Boolean.FALSE);
                        }))
                .defaultIfEmpty(Boolean.FALSE) // 确保 Transformer 失败时返回 FALSE
                .all(Boolean::booleanValue) // 确保所有 Transformer 都返回 true
                .flatMap(allSuccess -> {
                    if (!allSuccess) {
                        log.info("Skipping message {} due to transformer failure.", builder);
                        return Mono.empty();
                    }
                    return interceptMessage(builder.build());
                }));
    }

    private Mono<Message> interceptMessage(Message message) {
        return Mono.deferContextual(ctx -> Flux.fromIterable(messageInterceptors)
        .flatMap(interceptor -> interceptor.intercept(message, ctx))
        .last(message) // 取最后一个 Message，如果所有 interceptor 都成功
        .switchIfEmpty(Mono.error(new RuntimeException("No valid message after interception."))) // 为空时抛出异常
        .onErrorResume(ex -> {
            log.error("Interceptor failed, skipping message {}.", message, ex);
            return Mono.empty();
        }));
    }
}
