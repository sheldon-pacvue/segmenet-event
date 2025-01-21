package com.pacvue.segment.event.service.entry.sqs;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MessageAmazonSQSConsumer {
    @SqsListener("my-queue-name")  // 消费指定队列的消息
    public void handleMessage(String message) {
        log.info("Received SQS message: {}", message);
        // TODO 处理消息
    }
}
