package com.pacvue.segment.event.store;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.*;
import com.segment.analytics.gson.AutoValueAdapterFactory;
import com.segment.analytics.gson.ISO8601DateAdapter;
import com.segment.analytics.messages.Message;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Slf4j
@Data
@Builder
public class RabbitMQDistributedStore implements Store<Message> {
    private final String exchangeName;
    private final String routingKey;
    private final String queueName;

    private final Connection connection;
    private final Channel channel;
    private String consumerTag;



    // 发布消息
    public Mono<Boolean> publish(Message event) {
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
    @Override
    public void subscribe(Consumer<List<Message>> consumer, int bundleCount) {
        try {
            this.consumerTag = channel.basicConsume(queueName, true, new DefaultConsumer(channel) {
                @SneakyThrows
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    // 为了保留范型适配，这里必须进行类型强转，否则将导致类型转换错误
                    // 从消息头提取类型信息
                    String type = properties.getHeaders().get("type").toString();
                    Class<?> clazz = Class.forName(type);
                    // 动态加载类
                    Type resultType = TypeToken.get(clazz).getType();
                    Message event = gson.fromJson(new String(body, StandardCharsets.UTF_8), resultType);

                    List<Message> eventList = List.of(event);
                    consumer.accept(eventList);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Subscribing failed", e);
        }
    }

    @Override
    public void stopScribe() {
        // 停止消费（解除订阅）
        // 这里的 consumerTag 是上面 basicConsume 方法返回的标签
        try {
            channel.basicCancel(consumerTag);  // 取消消费者，解除订阅
            consumerTag = null;
        } catch (IOException e) {
            log.error("stopScribe failed", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            if (channel != null) {
                channel.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (IOException | TimeoutException ignore) {
        }
    }
}
