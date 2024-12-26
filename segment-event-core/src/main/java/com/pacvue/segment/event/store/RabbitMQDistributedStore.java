package com.pacvue.segment.event.store;

import cn.hutool.core.util.SerializeUtil;
import com.pacvue.segment.event.core.SegmentEvent;
import com.rabbitmq.client.*;
import lombok.Data;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Data
public class RabbitMQDistributedStore implements Store<SegmentEvent> {
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
    public Mono<Boolean> publish(SegmentEvent event) {
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
    public void subscribe(Consumer<List<SegmentEvent>> consumer, int consumeCountPer) {
        try {
            BatchPollQueue<SegmentEvent> buffer = new BatchPollQueue<>();
            channel.basicConsume(queueName, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    buffer.add(SerializeUtil.deserialize(body));
                    consumer.accept(buffer.poll(consumeCountPer));
                }

                @Override
                public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                    // 处理断开连接的逻辑，尝试重新连接
                    if (sig.isHardError()) {
                        System.out.println("Channel closed or lost connection, attempting to reconnect...");
                        while (true) {
                            List<SegmentEvent> events = buffer.poll(consumeCountPer);
                            if (events.isEmpty()) {
                                break;
                            }
                            consumer.accept(events);
                        }
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Subscribing failed", e);
        }
    }

    // 关闭连接
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
