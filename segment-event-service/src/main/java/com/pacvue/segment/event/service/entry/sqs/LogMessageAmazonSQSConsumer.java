package com.pacvue.segment.event.service.entry.sqs;

import com.pacvue.segment.event.entity.SegmentEventLogMessage;
import com.pacvue.segment.event.service.entity.po.SegmentEventLog;
import com.pacvue.segment.event.service.service.SegmentEventLogService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

@ConditionalOnProperty(value = "sqs.log.queue")
@Component
@Slf4j
public class LogMessageAmazonSQSConsumer {
    @Autowired
    private SegmentEventLogService service;

    @SqsListener("${sqs.log.queue}")  // 消费指定队列的消息
    public void handleMessage(SegmentEventLogMessage message) {
        log.info("Received SQS log message: {}", message);
        service.saveEventLog(SegmentEventLog.fromMessage(message))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }
}
