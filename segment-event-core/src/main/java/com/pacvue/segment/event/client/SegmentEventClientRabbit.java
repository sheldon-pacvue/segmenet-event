package com.pacvue.segment.event.client;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.segment.analytics.messages.Message;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Builder
@Slf4j
public class SegmentEventClientRabbit<T extends Message> extends AbstractBufferSegmentEventClient<T, SegmentEventClientRabbit<T>> {
    private final String exchangeName;
    private final String routingKey;
    private final String queueName;
    private final Channel channel;

    @Override
    public Mono<Boolean> send(List<T> events) {
        return Flux.fromIterable(events)
                .flatMap(event -> {
                    try {
                        // 创建消息属性
                        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                                .contentType("application/json")
                                .headers(Map.of("type", event.getClass().getName())) // 设置类型信息
                                .build();
                        channel.basicPublish(exchangeName, routingKey, props, gson.toJson(event).getBytes(StandardCharsets.UTF_8));
                        log.debug("send success, event：{}", event);
                        return Mono.just(Boolean.TRUE);
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                })
                .all(success -> success);
    }

    @Override
    public void flush() {

    }
}
