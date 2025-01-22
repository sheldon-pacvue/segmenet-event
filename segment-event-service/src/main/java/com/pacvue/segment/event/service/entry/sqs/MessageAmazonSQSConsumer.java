package com.pacvue.segment.event.service.entry.sqs;

import com.pacvue.segment.event.core.SegmentIO;
import com.segment.analytics.messages.Message;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(value = "sqs.message.queue")
@Component
@Slf4j
public class MessageAmazonSQSConsumer {
    @Autowired
    private SegmentIO segmentIO;

    @SqsListener("${sqs.message.queue}")  // 消费指定队列的消息
    public void handleMessage(Message message) {
        log.info("Received SQS message: {}", message);
        segmentIO.message(message);
    }
}
