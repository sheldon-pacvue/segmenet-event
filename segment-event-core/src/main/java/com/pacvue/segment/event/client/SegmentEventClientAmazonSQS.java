package com.pacvue.segment.event.client;

import com.segment.analytics.messages.Message;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

import java.util.List;
import java.util.stream.Collectors;

@Builder
@Slf4j
public class SegmentEventClientAmazonSQS<T extends Message> implements SegmentEventClient<T> {
    private final SqsAsyncClient client;
    private final String queueUrl;

    public SegmentEventClientAmazonSQS(String region, String queueUrl) {
        this.client = SqsAsyncClient.builder()
                .region(Region.of(region))  // 替换为你的AWS区域
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        this.queueUrl = queueUrl;
    }



    @Override
    public Mono<Boolean> send(List<T> events) {
        return Mono.fromFuture(() -> {
            // 将 events 转换为 SQS 批量发送请求
            List<SendMessageBatchRequestEntry> batchEntries = events.stream().map(event -> SendMessageBatchRequestEntry.builder()
                            .id(event.anonymousId())  // SQS 需要唯一 ID
                            .messageBody(event.toString())  // 转换为字符串
                            .build())
                    .collect(Collectors.toList());

            return client.sendMessageBatch(builder ->
                    builder.queueUrl(queueUrl).entries(batchEntries)
            );
        }).map(response -> {
            if (!response.failed().isEmpty()) {
                log.error("Failed to send some messages to SQS: {}", response.failed());
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        });
    }

    @Override
    public void flush() {

    }
}
