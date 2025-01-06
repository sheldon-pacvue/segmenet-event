package com.pacvue.segment.event.store;

import cn.hutool.core.util.SerializeUtil;
import com.pacvue.segment.event.generator.SegmentEvent;
import com.rabbitmq.client.*;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Slf4j
@Data
@Builder
public class RabbitMQDistributedStore implements Store<Void> {
    private final String exchangeName;
    private final String routingKey;
    private final String queueName;

    private final Connection connection;
    private final Channel channel;


    // 发布消息
    public Mono<Boolean> publish(SegmentEvent event, Void v) {
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
    public void subscribe(Consumer<List<SegmentEvent>> consumer, int bundleCount) {
        try {
            channel.basicConsume(queueName, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    // 为了保留范型适配，这里必须进行类型强转，否则将导致类型转换错误
                    @SuppressWarnings("unchecked")
                    List<SegmentEvent> eventList = List.of((SegmentEvent) SerializeUtil.deserialize(body));
                    consumer.accept(eventList);
                }
            });


        } catch (IOException e) {
            throw new RuntimeException("Subscribing failed", e);
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
