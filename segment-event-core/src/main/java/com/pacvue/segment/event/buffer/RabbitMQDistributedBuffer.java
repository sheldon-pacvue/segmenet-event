package com.pacvue.segment.event.buffer;


import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.*;
import com.segment.analytics.messages.Message;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Slf4j
@Builder
public class RabbitMQDistributedBuffer<T extends Message> extends AbstractBuffer<T> {
    private final String exchangeName;
    private final String routingKey;
    private final String queueName;

    private final Connection connection;
    private final Channel channel;


    // 发布消息
    @NotNull
    @Override
    public Mono<Boolean> commit(@NotNull T event) {
        return Mono.fromCallable(() -> {
            try {
                // 创建消息属性
                AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                        .contentType("application/json")
                        .headers(Map.of("type", event.getClass().getName())) // 设置类型信息
                        .build();
                channel.basicPublish(exchangeName, routingKey, props, gson.toJson(event).getBytes(StandardCharsets.UTF_8));
                log.debug("event publish success, event：{}", event);
                return Boolean.TRUE;
            } catch (IOException e) {
                log.debug("event publish failed, event：{}", event);
                throw new RuntimeException("Publishing failed", e);
            }
        });  // 可以映射为自定义异常
    }

    // 订阅消息
    @NotNull
    @Override
    protected StopAccept doAccept(@NotNull Consumer<List<T>> consumer) {
        try {
            String consumerTag = channel.basicConsume(queueName, true, new DefaultConsumer(channel) {
                @SneakyThrows
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    // 为了保留范型适配，这里必须进行类型强转，否则将导致类型转换错误
                    // 从消息头提取类型信息
                    String type = properties.getHeaders().get("type").toString();
                    Class<?> clazz = Class.forName(type);
                    Type resultType = TypeToken.get(clazz).getType();
                    T event = gson.fromJson(new String(body, StandardCharsets.UTF_8), resultType);

                    List<T> eventList = List.of(event);
                    consumer.accept(eventList);
                }
            });
            return () -> {
                try {
                    channel.basicCancel(consumerTag);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (IOException e) {
            throw new RuntimeException("Subscribing failed", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException | TimeoutException ignored) {
        }
    }
}
