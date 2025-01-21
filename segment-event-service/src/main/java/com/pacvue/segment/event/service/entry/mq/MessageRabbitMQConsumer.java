package com.pacvue.segment.event.service.entry.mq;

import com.segment.analytics.messages.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RabbitListener(queues = "my-stream")
public class MessageRabbitMQConsumer {
    public void consume(Message message) {
        log.info("message: {}", message);
        // TODO 这里可以做一些业务处理，将日志数据存入数据库，后续可以做一些数据分析或者补偿
    }
}
