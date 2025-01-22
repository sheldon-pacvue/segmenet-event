package com.pacvue.segment.event.client;

import cn.hutool.core.lang.UUID;
import com.pacvue.segment.event.gson.GsonConstant;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class SegmentEventClientAmazonSQS<T> extends AbstractBufferSegmentEventClient<T, SegmentEventClientAmazonSQS<T>> implements GsonConstant {
    private final CompletableFuture<String> queueUrl;
    private final SqsAsyncClient client;

    /**
     * private string $sqsUrl,
     * private string $sqsSecret,
     * private string $sqsKey,
     */
    public SegmentEventClientAmazonSQS(String region, String queueName, String awsAccessKey, String awsSecretKey) {
        this.client = SqsAsyncClient.builder()
                .region(Region.of(region))  // 替换为你的AWS区域
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(awsAccessKey, awsSecretKey)
                ))
                .build();
        this.queueUrl = getQueueUrl(queueName)
                .exceptionally(ex -> {
                    log.error("Failed to get SQS queue URL for {}: {}", queueName, ex.getMessage());
                    throw new RuntimeException("Failed to get queue URL", ex); // 让整个流程失败
                });
    }

    public CompletableFuture<String> getQueueUrl(String queueName) {
        return client.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(queueName)
                        .build())
                .thenApply(GetQueueUrlResponse::queueUrl);
    }


    @Override
    protected Mono<Boolean> send(List<T> events) {
        return Mono.fromFuture(() ->
            queueUrl.thenCompose(url -> {
                // 将 events 转换为 SQS 批量发送请求
                List<SendMessageBatchRequestEntry> batchEntries = events.stream().map(event -> SendMessageBatchRequestEntry.builder()
                                .id(UUID.fastUUID().toString(true))  // SQS 需要唯一 ID
                                .messageBody(gson.toJson(event))  // 转换为字符串
                                .build())
                        .collect(Collectors.toList());

                return client.sendMessageBatch(builder ->
                        builder.queueUrl(url).entries(batchEntries)
                );
            })
        ).map(response -> {
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

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private String region;
        private String queueName;
        private String awsAccessKey;
        private String awsSecretKey;

        public Builder<T> region(String region) {
            this.region = region;
            return this;
        }

        public Builder<T> queueName(String queueName) {
            this.queueName = queueName;
            return this;
        }

        public Builder<T> awsAccessKey(String awsAccessKey) {
            this.awsAccessKey = awsAccessKey;
            return this;
        }

        public Builder<T> awsSecretKey(String awsSecretKey) {
            this.awsSecretKey = awsSecretKey;
            return this;
        }

        public SegmentEventClientAmazonSQS<T> build() {
            return new SegmentEventClientAmazonSQS<>(region, queueName, awsAccessKey, awsSecretKey);
        }
    }
}
