package com.pacvue.segment.event.service.configuration;

import com.pacvue.segment.event.client.SegmentEventClientAnalytics;
import com.pacvue.segment.event.client.SegmentEventClientClickHouse;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.entity.SegmentLogMessage;
import com.pacvue.segment.event.extend.ReactorMessageTransformer;
import com.pacvue.segment.event.metric.MetricsCounter;
import com.pacvue.segment.event.spring.metrics.SpringPrometheusMetricsCounter;
import com.pacvue.segment.event.springboot.properties.PrometheusMetricsProperties;
import com.pacvue.segment.event.springboot.properties.SegmentEventClientProperties;
import com.segment.analytics.messages.Message;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SegmentEventConfiguration {
    /**
     * prometheus指标计数器
     *
     * @param meterRegistry 指标注册器
     * @param properties 配置
     * @return 指标计数器
     */
    @Bean
    public MetricsCounter metricsCounter(MeterRegistry meterRegistry, PrometheusMetricsProperties properties) {
        return SpringPrometheusMetricsCounter.builder(meterRegistry, properties.getName())
                .tags(properties.getTags())
                .build();
    }

    /**
     * 上报器
     *
     * @param client 官方提供的客户端
     * @param metricsCounter 指标计数器
     * @return 事件上报器
     */
    @Bean
    public SegmentEventReporter segmentEventReporter(SegmentEventClientAnalytics<Message> client, MetricsCounter metricsCounter) {
        return SegmentEventReporter.builder()
                .client(client)
                .metricsCounter(metricsCounter)
                .build();
    }

    /**
     * 核心工具
     *
     * @param properties 配置
     * @param segmentEventReporter 上报器
     * @param eventLogger 事件日志记录器
     * @param transformers 转换器
     * @return 客户端
     */
    @Bean
    public SegmentIO segmentIO(SegmentEventClientProperties properties, SegmentEventReporter segmentEventReporter, SegmentEventClientClickHouse<SegmentLogMessage> eventLogger, List<ReactorMessageTransformer> transformers) {
        return SegmentIO.builder()
                .reporter(segmentEventReporter)
                .eventLogger(eventLogger)
                .secret(properties.getSecret())
                .reportApp(properties.getAppId())
                .messageTransformers(transformers)
                .build();
    }
}
