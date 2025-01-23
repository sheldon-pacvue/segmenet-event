package com.pacvue.segment.event.service.configuration;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("rabbitmq.log.queue")
public class RabbitMQLogMessageConfig {
    @Value("${rabbitmq.log.queue}")
    private String logQueueName;

    // 配置队列和其死信队列
    @Bean
    public Queue logQueue() {
        return QueueBuilder.durable(logQueueName)
                .exclusive()
                .withArgument("x-dead-letter-exchange", "dlx-exchange")
                .withArgument("x-dead-letter-routing-key", "log-queue-dlq")
                .build();
    }

    // 配置死信队列
    @Bean
    public Queue logDeadLetterQueue() {
        return QueueBuilder.durable(logQueueName + "-dlq").build();
    }

    // 配置普通交换机
    @Bean
    public DirectExchange logExchange() {
        return new DirectExchange("log-exchange");
    }

    // 配置死信交换机
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange("dlx-exchange");
    }

    // 配置绑定普通队列到交换机
    @Bean
    public Binding logBinding() {
        return BindingBuilder.bind(logQueue()).to(logExchange()).with("log-queue");
    }

    // 配置死信队列到死信交换机
    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(logDeadLetterQueue()).to(dlxExchange()).with("log-queue-dlq");
    }
}