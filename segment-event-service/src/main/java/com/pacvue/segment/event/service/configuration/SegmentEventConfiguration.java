package com.pacvue.segment.event.service.configuration;

import com.pacvue.segment.event.buffer.DefaultBuffer;
import com.pacvue.segment.event.client.SegmentEventClient;
import com.pacvue.segment.event.client.SegmentEventClientKafka;
import com.pacvue.segment.event.client.SegmentEventClientMybatisFlex;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.extend.ReactorMessageTransformer;
import com.pacvue.segment.event.metric.MetricsCounter;
import com.pacvue.segment.event.service.entity.po.SegmentEventLog;
import com.pacvue.segment.event.service.mapper.SegmentEventLogMapper;
import com.pacvue.segment.event.spring.metrics.SpringPrometheusMetricsCounter;
import com.pacvue.segment.event.springboot.properties.ClientProperties;
import com.pacvue.segment.event.springboot.properties.LoggerProperties;
import com.pacvue.segment.event.springboot.properties.PrometheusMetricsProperties;
import com.pacvue.segment.event.springboot.properties.ReporterProperties;
import com.segment.analytics.messages.Message;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Objects;

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

    @Bean
    public SegmentEventClientMybatisFlex<SegmentEventLog> segmentEventLogger(ObjectProvider<LoggerProperties> properties, SqlSessionFactory sessionFactory) {
        LoggerProperties loggerProperties = properties.getIfAvailable(LoggerProperties::new);
        SegmentEventClientMybatisFlex<SegmentEventLog> eventLogger = SegmentEventClientMybatisFlex.<SegmentEventLog>builder()
                .sqlSessionFactory(sessionFactory)
                .mapperClass(SegmentEventLogMapper.class)
                .isSupportValues(false)
                .build();
        eventLogger.buffer(DefaultBuffer.<SegmentEventLog>builder()
                .bufferSize(loggerProperties.getBufferSize())
                .flushInterval(loggerProperties.getFlushInterval())
                .build());
        return eventLogger;
    }

    @Bean
    public SegmentEventClientKafka<Message> segmentEventClient(ObjectProvider<ClientProperties> properties) {
        return SegmentEventClientKafka.<Message>builder()
                .topic(Objects.requireNonNull(properties.getIfAvailable()).getKafka().getTopic())
                .producer(new KafkaProducer<>(Objects.requireNonNull(properties.getIfAvailable().getKafka()).getProperties()))
                .build();
    }

    /**
     * 上报器
     *
     * @param segmentEventClient 上报客户端
     * @param segmentEventLogger 日志记录器
     * @param metricsCounter 指标计数器
     * @return 事件上报器
     */
    @Bean
    public SegmentEventReporter<SegmentEventLog> segmentEventReporter(ReporterProperties properties,
                                                                      SegmentEventClient<Message> segmentEventClient,
                                                                      SegmentEventClient<SegmentEventLog> segmentEventLogger,
                                                                      MetricsCounter metricsCounter) {
        return SegmentEventReporter.<SegmentEventLog>builder()
                .reportOperation(SegmentEventReporter.LOG_OPERATION_SEND_TO_DIRECT)
                .sender(segmentEventClient)
                .senderLimitCount(properties.getSenderLimitCount())
                .logClass(SegmentEventLog.class)
                .eventLogger(segmentEventLogger)
                .loggerLimitCount(properties.getLoggerLimitCount())
                .metricsCounter(metricsCounter)
                .build();
    }

    /**
     * 核心工具
     *
     * @param segmentEventReporter 上报器
     * @param transformers 转换器
     * @return 客户端
     */
    @Bean
    public SegmentIO segmentIO(SegmentEventReporter<SegmentEventLog> segmentEventReporter,
                               List<ReactorMessageTransformer> transformers) {
        return SegmentIO.builder()
                .reporter(segmentEventReporter)
                .messageTransformers(transformers)
                .build();
    }
}
