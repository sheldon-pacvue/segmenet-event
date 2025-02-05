package com.pacvue.segment.event.service.configuration;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.pacvue.segment.event.buffer.DefaultBuffer;
import com.pacvue.segment.event.client.SegmentEventClient;
import com.pacvue.segment.event.client.SegmentEventClientDataSource;
import com.pacvue.segment.event.client.SegmentEventClientMybatisFlex;
import com.pacvue.segment.event.client.SegmentEventClientSendReject;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.entity.SegmentEventLogMessage;
import com.pacvue.segment.event.extend.ReactorMessageTransformer;
import com.pacvue.segment.event.metric.MetricsCounter;
import com.pacvue.segment.event.service.entity.po.SegmentEventLog;
import com.pacvue.segment.event.service.mapper.SegmentEventLogMapper;
import com.pacvue.segment.event.spring.metrics.SpringPrometheusMetricsCounter;
import com.pacvue.segment.event.springboot.properties.LoggerProperties;
import com.pacvue.segment.event.springboot.properties.PrometheusMetricsProperties;
import com.pacvue.segment.event.springboot.properties.impl.DataSourceProperties;
import com.segment.analytics.messages.Message;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

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
    public SegmentEventClientDataSource<SegmentEventLogMessage> segmentEventLogger(ObjectProvider<LoggerProperties> properties, DataSource dataSource) {
        LoggerProperties loggerProperties = properties.getIfAvailable(LoggerProperties::new);
        DataSourceProperties clickhouse = Optional.ofNullable(loggerProperties.getDatasource()).orElseGet(DataSourceProperties::new);
        SegmentEventClientDataSource<SegmentEventLogMessage> eventLogger = SegmentEventClientDataSource.<SegmentEventLogMessage>builder()
                .dataSource(dataSource)
                .insertSql(clickhouse.getInsertSql())
                .argumentsConverter(event -> new Object[]{
                        Optional.ofNullable(DateUtil.date(event.eventTime())).map(DateTime::toSqlDate).orElse(null),
                        DigestUtil.md5Hex(event.message()),
                        event.userId(),
                        event.type(),
                        event.message(),
                        event.reported(),
                        event.operation(),
                        DateUtil.date().getTime() / 1000,
                        event.eventTime().getTime() / 1000
                })
                .build();
        if (loggerProperties.getBufferSize() > 0 && loggerProperties.getFlushInterval() > 0) {
            eventLogger.buffer(DefaultBuffer.<SegmentEventLogMessage>builder().bufferSize(loggerProperties.getBufferSize()).flushInterval(loggerProperties.getFlushInterval()).build());
        }
        return eventLogger;
    }

//    @Bean
//    public SegmentEventClientMybatisFlex<SegmentEventLogMessage, SegmentEventLog> segmentEventLogger(ObjectProvider<LoggerProperties> properties, SqlSessionFactory sessionFactory) {
//        LoggerProperties loggerProperties = properties.getIfAvailable(LoggerProperties::new);
//        SegmentEventClientMybatisFlex<SegmentEventLogMessage, SegmentEventLog> eventLogger = SegmentEventClientMybatisFlex.<SegmentEventLogMessage, SegmentEventLog>builder()
//                .sqlSessionFactory(sessionFactory)
//                .mapperClass(SegmentEventLogMapper.class)
//                .argumentsConverter(SegmentEventLog::fromMessage)
//                .isSupportValues(false)
//                .build();
//        eventLogger.buffer(DefaultBuffer.<SegmentEventLogMessage>builder()
//                .bufferSize(loggerProperties.getBufferSize())
//                .flushInterval(loggerProperties.getFlushInterval())
//                .build());
//        return eventLogger;
//    }



    @Bean
    public SegmentEventClient<Message> segmentEventClient() {
        return SegmentEventClientSendReject.<Message>builder().build();
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
    public SegmentEventReporter segmentEventReporter(SegmentEventClient<Message> segmentEventClient,
                                                     SegmentEventClient<SegmentEventLogMessage> segmentEventLogger,
                                                     MetricsCounter metricsCounter) {
        return SegmentEventReporter.builder()
                .reportOperation(SegmentEventReporter.LOG_OPERATION_SEND_TO_DIRECT)
                .client(segmentEventClient)
                .eventLogger(segmentEventLogger)
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
    public SegmentIO segmentIO(SegmentEventReporter segmentEventReporter,
                               List<ReactorMessageTransformer> transformers) {
        return SegmentIO.builder()
                .reporter(segmentEventReporter)
                .messageTransformers(transformers)
                .build();
    }
}
