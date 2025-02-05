package com.pacvue.segment.event.springboot.configuration;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.pacvue.segment.event.buffer.DefaultBuffer;
import com.pacvue.segment.event.client.SegmentEventClient;
import com.pacvue.segment.event.client.SegmentEventClientAnalytics;
import com.pacvue.segment.event.client.SegmentEventClientDataSource;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.entity.SegmentEventLogMessage;
import com.pacvue.segment.event.springboot.properties.ClientProperties;
import com.pacvue.segment.event.springboot.properties.LoggerProperties;
import com.pacvue.segment.event.springboot.properties.impl.AnalyticsProperties;
import com.pacvue.segment.event.springboot.properties.impl.DataSourceProperties;
import com.pacvue.segment.event.extend.ReactorMessageInterceptor;
import com.pacvue.segment.event.extend.ReactorMessageTransformer;
import com.segment.analytics.Analytics;
import com.segment.analytics.Log;
import com.segment.analytics.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Configuration
@ComponentScan(basePackages = "com.pacvue")
@ConfigurationPropertiesScan(basePackages = {
        "com.pacvue.segment.event.springboot.properties"
})
public class SegmentEventAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(name = "segmentEventClient")
    public SegmentEventClient<Message> segmentEventClient(ClientProperties properties) throws NoSuchFieldException, IllegalAccessException {
        AnalyticsProperties analyticsProperties = properties.getAnalytics();
        Logger log = LoggerFactory.getLogger(Analytics.class);
        Analytics analytics = Analytics.builder(analyticsProperties.getWriteKey())
                .log(new Log() {
                    @Override
                    public void print(Level level, String format, Object... args) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format(format, args));
                        }
                    }

                    @Override
                    public void print(Level level, Throwable error, String format, Object... args) {
                        log.error(String.format(format, args), error);
                    }
                }).build();
        return SegmentEventClientAnalytics.builder().analytics(analytics).build();
    }

    /**
     * CREATE TABLE default.SegmentEventLog
     * (
     *     `eventDate` Date,
     *     `hash` String,
     *     `userId` String,
     *     `type` String,
     *     `message` String,
     *     `result` UInt8,
     *     `operation` UInt8,
     *     `createdAt` Int32,
     *     `eventTime` Int32
     * )
     * ENGINE = ReplicatedMergeTree('/clickhouse/tables/{shard}/SegmentEventsLog',
     *  '{replica}')
     * PARTITION BY toYYYYMM(eventDate)
     * ORDER BY (userId,
     *  eventDate,
     *  type,
     *  result)
     * SETTINGS index_granularity = 8192;
     */
    @Bean
    @ConditionalOnMissingBean(name = "segmentEventLogger")
    public SegmentEventClient<SegmentEventLogMessage> segmentEventLogger(ObjectProvider<LoggerProperties> loggerProperties) {
        LoggerProperties properties = loggerProperties.getIfAvailable(LoggerProperties::new);
        DataSourceProperties clickhouse = properties.getDatasource();
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.configFromPropeties(clickhouse.getDataSourceProperties());

        return SegmentEventClientDataSource.<SegmentEventLogMessage>builder()
                .dataSource(druidDataSource)
                .insertSql(clickhouse.getInsertSql())
                .argumentsConverter(event -> new Object[]{
                        Optional.ofNullable(DateUtil.date(event.eventTime())).map(DateTime::toSqlDate).orElse(null),
                        DigestUtil.md5Hex(event.toString()),
                        event.userId(),
                        event.type(),
                        event.toString(),
                        event.reported(),
                        event.operation(),
                        DateUtil.date().getTime() / 1000,
                        event.eventTime().getTime() / 1000
                })
                .build()
                .buffer(DefaultBuffer.<SegmentEventLogMessage>builder().bufferSize(properties.getBufferSize()).build());
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentEventReporter segmentEventReporter(@Qualifier("segmentEventClient") SegmentEventClient<Message> segmentEventClient,
                                                     @Qualifier("segmentEventLogger") SegmentEventClient<SegmentEventLogMessage> segmentEventLogger) {
        return SegmentEventReporter.builder()
                .reportOperation(SegmentEventReporter.LOG_OPERATION_SEND_TO_DIRECT)
                .client(segmentEventClient)
                .eventLogger(segmentEventLogger)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentIO segmentIO(SegmentEventReporter segmentEventReporter,
                               ObjectProvider<List<ReactorMessageTransformer>> transformers,
                               ObjectProvider<List<ReactorMessageInterceptor>> interceptors) {
        return SegmentIO.builder()
                .reporter(segmentEventReporter)
                .messageTransformers(transformers.getIfAvailable(ArrayList::new))
                .messageInterceptors(interceptors.getIfAvailable(ArrayList::new))
                .build();
    }
}
