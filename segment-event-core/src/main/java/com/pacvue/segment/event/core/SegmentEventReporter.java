package com.pacvue.segment.event.core;

import com.pacvue.segment.event.client.SegmentEventClient;
import com.pacvue.segment.event.entity.MessageLog;
import com.pacvue.segment.event.gson.GsonConstant;
import com.pacvue.segment.event.helper.MethodConcurrentLimitHelper;
import com.pacvue.segment.event.metric.MetricsCounter;
import com.segment.analytics.messages.Message;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;


@Slf4j
@Data
@Builder
public final class SegmentEventReporter<T extends MessageLog<T>> implements GsonConstant {
    /**
     * 这里定义了两种上报操作
     * 如果report是上报给一个异步队列，比如rabbitmq，amazon sqs或者第三方平台，则使用 LOG_OPERATION_SEND_TO_INDIRECT
     * 如果report是直接上报给segment.io，则使用 LOG_OPERATION_SEND_TO_DIRECT
     */
    public final static short LOG_OPERATION_SEND_TO_INDIRECT = 1;
    public final static short LOG_OPERATION_SEND_TO_DIRECT = 2;

    private final short reportOperation;
    @Getter
    private final MetricsCounter metricsCounter;
    @NonNull
    private SegmentEventClient<Message> sender;
    private int senderLimitCount;
    @NonNull
    private final Class<T> logClass;
    @NonNull
    private SegmentEventClient<T> eventLogger;
    private int loggerLimitCount;

    @SuppressWarnings("unchecked")
    public void init() {
        sender = MethodConcurrentLimitHelper.wrap(sender, SegmentEventClient.class, "send", senderLimitCount);
        eventLogger = MethodConcurrentLimitHelper.wrap(eventLogger, SegmentEventClient.class, "send", loggerLimitCount);
    }

    public Mono<Boolean> report(Message... events) {
        return sender.send(events)
                .flatMap(b -> {
                    /*
                       事件id是helium10.segmentio.async.send-events
                       内容写入到 @console/runtime/telegraf-metrics.out
                       内容类似 name+methods value timestamp
                       helium10.segmentio.async.send-events.count 100 1245547899
                     */
                    log.debug("report success, data: {}, result: {}", events, b);
                    Optional.ofNullable(metricsCounter).ifPresent(counter -> counter.inc(events.length));
                    // 处理所有事件日志，等待全部完成
                    return Flux.fromArray(events)
                            .flatMap(event -> tryLog(event, true)) // 并行执行
                            .all(success -> success); // 所有日志完成后，返回 true
                })
                .onErrorResume(throwable -> {
                    log.debug("report failed, switching to single event processing.", throwable);
                    return Flux.fromArray(events)
                            .flatMap(event -> tryLog(event, false)
                                    .onErrorReturn(false)) // 任意失败返回 false
                            .all(success -> success) // 只要有 false 就返回 false
                            .defaultIfEmpty(Boolean.TRUE); // 处理空集合情况;
                })
                .doFinally(signalType -> {
                    log.debug("一批数据结束");
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public void flush() {
        sender.flush();
        eventLogger.flush();
    }

    private Mono<Boolean> tryLog(Message event, boolean result) {
        T logs = getLogInstance().covert(event).result(result).operation(reportOperation);
        return eventLogger.send(logs)
                .onErrorResume(ex -> {
                    log.warn("try log failed, event: {}", event, ex);
                    return Mono.just(Boolean.FALSE);
                });
    }

    public T getLogInstance() {
        try {
            // 通过无参构造方法创建实例
            return logClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("get log instance failed");
        }
    }
}
