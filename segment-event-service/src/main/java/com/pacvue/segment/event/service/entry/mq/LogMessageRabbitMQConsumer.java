package com.pacvue.segment.event.service.entry.mq;

import com.pacvue.segment.event.entity.SegmentLogMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RabbitListener(queues = "my-stream")
public class LogMessageRabbitMQConsumer {
    public void consume(SegmentLogMessage message) {
        log.info("log message: {}", message);
        // TODO 这里可以做一些业务处理，将日志数据存入数据库，后续可以做一些数据分析或者补偿
    }
}
