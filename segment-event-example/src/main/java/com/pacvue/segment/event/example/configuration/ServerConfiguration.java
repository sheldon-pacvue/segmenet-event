package com.pacvue.segment.event.example.configuration;


import com.pacvue.segment.event.client.*;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.entity.SegmentEventLogMessage;
import com.pacvue.segment.event.metric.MetricsCounter;
import com.pacvue.segment.event.spring.filter.ReactorRequestHolderFilter;
import com.pacvue.segment.event.spring.metrics.SpringPrometheusMetricsCounter;
import com.pacvue.segment.event.spring.transformer.UserIdReactorMessageTransformer;
import com.pacvue.segment.event.springboot.properties.*;
import com.pacvue.segment.event.springboot.properties.impl.*;
import com.pacvue.segment.event.extend.ReactorMessageTransformer;
import com.segment.analytics.messages.Message;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ServerConfiguration {

    @Bean
    public ReactorRequestHolderFilter requestHolderFilter() {
        return new ReactorRequestHolderFilter();
    }

    @Bean
    public MetricsCounter metricsCounter(MeterRegistry meterRegistry, PrometheusMetricsProperties properties) {
        return SpringPrometheusMetricsCounter.builder(meterRegistry, properties.getName())
                .tags(properties.getTags())
                .build();
    }

    @Bean
    public SegmentEventClientAmazonSQS<SegmentEventLogMessage> segmentEventLogger(LoggerProperties properties) {
        AmazonSQSProperties sqs = properties.getAmazonSqs();

        return SegmentEventClientAmazonSQS.<SegmentEventLogMessage>builder()
                .region(sqs.getRegion())
                .awsAccessKey(sqs.getAwsAccessKey())
                .awsSecretKey(sqs.getAwsSecretKey())
                .queueName(sqs.getQueueName())
                .build();
    }

    @Bean
    public SegmentEventReporter segmentEventReporter(SegmentEventClient<Message> client,
                                                     SegmentEventClient<SegmentEventLogMessage> eventLogger,
                                                     MetricsCounter metricsCounter) {
        return SegmentEventReporter.builder()
                .reportOperation(SegmentEventReporter.LOG_OPERATION_SEND_TO_DIRECT)
                .client(client)
                .eventLogger(eventLogger)
                .metricsCounter(metricsCounter)
                .build();
    }



    @Bean
    public UserIdReactorMessageTransformer userIdReactorMessageTransformer() {
        return new UserIdReactorMessageTransformer("X-User-ID");
    }


    @Bean
    public SegmentIO segmentIO(SegmentEventReporter segmentEventReporter,
                               List<ReactorMessageTransformer> transformers) {
        return SegmentIO.builder()
                .reporter(segmentEventReporter)
                .messageTransformers(transformers)
                .build();
    }
}
