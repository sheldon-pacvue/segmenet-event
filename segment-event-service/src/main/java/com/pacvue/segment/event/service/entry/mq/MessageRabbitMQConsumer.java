package com.pacvue.segment.event.service.entry.mq;

import com.pacvue.segment.event.core.SegmentIO;
import com.segment.analytics.messages.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RabbitListener(queues = "${rabbitmq.message.queue}", autoStartup = "true")
@ConditionalOnProperty(value = "rabbitmq.message.queue")
public class MessageRabbitMQConsumer {
    @Autowired
    private SegmentIO segmentIO;

    @RabbitHandler
    public void consume(Message message) {
        log.info("message: {}", message);
        segmentIO.message(message);
    }
}
