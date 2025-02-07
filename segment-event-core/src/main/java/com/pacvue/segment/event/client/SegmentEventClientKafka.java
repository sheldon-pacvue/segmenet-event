package com.pacvue.segment.event.client;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Builder
@Slf4j
public class SegmentEventClientKafka<T> extends AbstractBufferSegmentEventClient<T, SegmentEventClientKafka<T>> {
    @NonNull
    private final String topic;
    @NonNull
    private final KafkaProducer<String, String> producer;

    @Override
    public Mono<Boolean> send(List<T> events) {
        return Flux.fromIterable(events)
                .flatMap(event -> Mono.<Boolean>create(sink -> {
                    ProducerRecord<String, String> record = new ProducerRecord<>(topic, gson.toJson(event));
                    producer.send(record, (metadata, exception) -> {
                        if (exception != null) {
                            sink.error(exception);  // 异常处理
                        } else {
                            log.debug("send success, event：{}", event);  // 发送成功的日志
                            sink.success(Boolean.TRUE);  // 发送成功
                        }
                    });
                }))
                .all(success -> success)  // 检查是否所有消息发送成功
                .defaultIfEmpty(Boolean.TRUE);  // 如果没有事件，返回 true
    }
}
