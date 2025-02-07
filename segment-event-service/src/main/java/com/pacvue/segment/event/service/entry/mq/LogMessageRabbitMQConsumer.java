package com.pacvue.segment.event.service.entry.mq;

import com.pacvue.segment.event.entity.SegmentEventLogMessage;
import com.pacvue.segment.event.service.entity.po.SegmentEventLog;
import com.pacvue.segment.event.service.service.SegmentEventLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RabbitListener(queues = "${rabbitmq.log.queue}", autoStartup = "true")
@ConditionalOnProperty(value = "rabbitmq.log.queue")
public class LogMessageRabbitMQConsumer {
    @Autowired
    private SegmentEventLogService service;

    @RabbitHandler
    public void consume(SegmentEventLogMessage message) {
        log.info("log message: {}", message);
        service.saveEventLog(new SegmentEventLog().covert(message))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }
}
