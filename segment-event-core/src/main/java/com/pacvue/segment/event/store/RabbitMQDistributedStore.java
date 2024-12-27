package com.pacvue.segment.event.store;

import cn.hutool.core.util.SerializeUtil;
import com.rabbitmq.client.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
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
            BatchPollQueue<T> buffer = new BatchPollQueue<>();
            channel.basicConsume(queueName, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    buffer.add(SerializeUtil.deserialize(body));
                    wrapConsume(consumer, buffer.poll(bundleCount));
                }

                @Override
                public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                    // 处理断开连接的逻辑，尝试重新连接
                    if (sig.isHardError()) {
                        log.warn("Channel closed or lost connection, attempting to reconnect...");
                        while (true) {
                            List<T> events = buffer.poll(bundleCount);
                            if (events.isEmpty()) {
                                break;
                            }
                            wrapConsume(consumer, events);
                        }
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Subscribing failed", e);
        }
    }

    public void wrapConsume(Consumer<List<T>> consumer, List<T> events) {
        log.debug("event consume start, events：{}", events);
        if (events.isEmpty()) {
            return;
        }
        consumer.accept(events);
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


    protected static class BatchPollQueue<T> extends LinkedBlockingQueue<T> {

        public synchronized List<T> poll(int count) {
            List<T> list = new ArrayList<>();
            if (this.size() < count) {
                return list;
            }
            while (count-- > 0) {
                T t = poll();
                if (t != null) {
                    list.add(t);
                }
            }
            return list;
        }
    }

}
