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

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            this.connection = connection;
            this.channel = channel;
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("RabbitMQ connection failed", e);
        }
    }

    // 发布消息
    public Mono<Boolean> publish(T event) {
        return Mono.fromCallable(() -> {
            try {
                channel.basicPublish(exchangeName, routingKey, null, SerializeUtil.serialize(event));
                return Boolean.TRUE;
            } catch (IOException e) {
                throw new RuntimeException("Publishing failed", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());  // 可以映射为自定义异常
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

    public void wrapConsume(Consumer<List<T>> consumer, List<T> data) {
        try {
            consumer.accept(data);
        } catch (Throwable e) {
            log.error("consume failed, data: {}", data, e);
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
