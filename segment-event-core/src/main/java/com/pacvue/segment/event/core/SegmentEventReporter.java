package com.pacvue.segment.event.core;

import cn.hutool.crypto.digest.DigestUtil;
import com.pacvue.segment.event.client.SegmentEventClient;
import com.pacvue.segment.event.entity.SegmentEventLogMessage;
import com.pacvue.segment.event.gson.GsonConstant;
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
public final class SegmentEventReporter implements GsonConstant {
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
    private final SegmentEventClient<Message> client;
    @NonNull
    private final SegmentEventClient<SegmentEventLogMessage> eventLogger;

    public Mono<Boolean> report(Message... events) {
        return client.send(events)
                .doOnSuccess(b -> {
                    /*
                       事件id是helium10.segmentio.async.send-events
                       内容写入到 @console/runtime/telegraf-metrics.out
                       内容类似 name+methods value timestamp
                       helium10.segmentio.async.send-events.count 100 1245547899
                     */
                    log.debug("report success, data: {}, result: {}", events, b);
                    Optional.ofNullable(metricsCounter).ifPresent(counter -> counter.inc(events.length));
                    for (Message event : events) {
                        tryLog(event, true).subscribe();
                    }
                })
                .onErrorResume(throwable -> {
                    log.debug("report failed, switching to single event processing.", throwable);
                    return Flux.fromArray(events)
                            .flatMap(event -> tryLog(event, false)
                                    .onErrorReturn(false)) // 任意失败返回 false
                            .all(success -> success) // 只要有 false 就返回 false
                            .defaultIfEmpty(Boolean.TRUE); // 处理空集合情况;
                })
                .subscribeOn(Schedulers.boundedElastic());

    }

    public void flush() {
        client.flush();
        eventLogger.flush();
    }

    private Mono<Boolean> tryLog(Message event, boolean result) {
        String message = gson.toJson(event);
        String hash = DigestUtil.md5Hex(message);

        return eventLogger.send(SegmentEventLogMessage.builder()
                        .eventTime(event.timestamp())
                        .message(message)
                        .userId(event.userId())
                        .type(event.type().name())
                        .hash(hash)
                        .reported(result)
                        .operation(reportOperation)
                        .build())
                .onErrorResume(ex -> {
                    log.warn("try log failed, event: {}", event, ex);
                    return Mono.just(Boolean.FALSE);
                });
    }
}
