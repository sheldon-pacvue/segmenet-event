package com.pacvue.segment.event.store;

import cn.hutool.core.util.SerializeUtil;
import com.rabbitmq.client.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Slf4j
@Data
public class RabbitMQDistributedStore<T> implements Store<T> {
    private final String exchangeName;
    private final String routingKey;
    private final String queueName;

    private final Connection connection;
    private final Channel channel;


    // 初始化连接和频道
    public RabbitMQDistributedStore(ConnectionFactory factory, String exchangeName, String routingKey, String queueName) {
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
        this.queueName = queueName;

        try {
            this.connection = factory.newConnection();
            this.channel = connection.createChannel();
            channel.queueDeclare(queueName, false, false, false, null);
            channel.queueBind(queueName, exchangeName, routingKey);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("RabbitMQ connection failed", e);
        }
    }

    // 发布消息
    public Mono<Boolean> publish(T event) {
        return Mono.fromCallable(() -> {
            try {
                channel.basicPublish(exchangeName, routingKey, null, SerializeUtil.serialize(event));
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
    public void subscribe(Consumer<List<T>> consumer, int bundleCount) {
        try {
            channel.basicConsume(queueName, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    // 为了保留范型适配，这里必须进行类型强转，否则将导致类型转换错误
                    @SuppressWarnings("unchecked")
                    List<T> eventList = List.of((T) SerializeUtil.deserialize(body));
                    consumer.accept(eventList);
                }
            });


        } catch (IOException e) {
            throw new RuntimeException("Subscribing failed", e);
        }
    }

    public void close() {
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
