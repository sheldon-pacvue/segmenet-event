package com.pacvue.segment.event.service.configuration;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("rabbitmq.message.queue")
public class RabbitMQMessageConfig {
    @Value("${rabbitmq.message.queue}")
    private String messageQueueName;

    // 配置队列和其死信队列
    @Bean
    public Queue messageQueue() {
        return QueueBuilder.durable(messageQueueName)
                .exclusive()
                .withArgument("x-dead-letter-exchange", "message-dlx-exchange")
                .withArgument("x-dead-letter-routing-key", "message-queue-dlq")
                .build();
    }

    // 配置死信队列
    @Bean
    public Queue messageDeadLetterQueue() {
        return QueueBuilder.durable(messageQueueName + "-dlq").build();
    }

    // 配置普通交换机
    @Bean
    public DirectExchange messageExchange() {
        return new DirectExchange("message-exchange");
    }

    // 配置死信交换机
    @Bean
    public DirectExchange messageDlxExchange() {
        return new DirectExchange("message-dlx-exchange");
    }

    // 配置绑定普通队列到交换机
    @Bean
    public Binding messageBinding() {
        return BindingBuilder.bind(messageQueue()).to(messageExchange()).with("message-queue");
    }

    // 配置死信队列到死信交换机
    @Bean
    public Binding messageDlxBinding() {
        return BindingBuilder.bind(messageDeadLetterQueue()).to(messageDlxExchange()).with("message-queue-dlq");
    }
}