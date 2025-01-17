package com.pacvue.segment.event.springboot.configuration;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.pacvue.segment.event.client.SegmentEventClientAnalytics;
import com.pacvue.segment.event.client.SegmentEventClientClickHouse;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.entity.SegmentLogMessage;
import com.pacvue.segment.event.springboot.properties.LoggerProperties;
import com.pacvue.segment.event.springboot.properties.SegmentEventClientProperties;
import com.pacvue.segment.event.springboot.properties.impl.ClickHouseProperties;
import com.pacvue.segment.event.extend.ReactorMessageInterceptor;
import com.pacvue.segment.event.extend.ReactorMessageTransformer;
import com.segment.analytics.Analytics;
import com.segment.analytics.Log;
import com.segment.analytics.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

@Configuration
@ComponentScan(basePackages = "com.pacvue")
@ConfigurationPropertiesScan(basePackages = {
        "com.pacvue.segment.event.springboot.properties"
})
public class SegmentEventAutoConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = SegmentEventClientProperties.PROPERTIES_PREFIX, name = "secret")
    @ConditionalOnMissingBean
    public Analytics segmentAnalytics(SegmentEventClientProperties properties) {
        Logger log = LoggerFactory.getLogger(Analytics.class);
        return Analytics.builder(properties.getSecret())
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
    }

    @Bean
    @ConditionalOnBean(Analytics.class)
    @ConditionalOnMissingBean
    public SegmentEventClientAnalytics<Message> segmentEventClientAnalytics(Analytics segmentAnalytics) throws NoSuchFieldException, IllegalAccessException {
        return SegmentEventClientAnalytics.builder().analytics(segmentAnalytics).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentEventReporter segmentEventReporter(SegmentEventClientAnalytics<Message> segmentEventClientAnalytics) {
        return SegmentEventReporter.builder().client(segmentEventClientAnalytics).build();
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
    public SegmentEventClientClickHouse<SegmentLogMessage> eventLogger(LoggerProperties properties) {
        ClickHouseProperties clickhouse = properties.getClickhouse();
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.configFromPropeties(clickhouse.getDataSourceProperties());

        return SegmentEventClientClickHouse.<SegmentLogMessage>builder()
                .dataSource(druidDataSource)
                .insertSql(clickhouse.getInsertSql())
                .argumentsConverter(event -> new Object[]{
                        Optional.ofNullable(DateUtil.date(event.sentAt())).map(DateTime::toSqlDate).orElse(null),
                        DigestUtil.md5Hex(event.toString()),
                        event.userId(),
                        event.type().name(),
                        event.toString(),
                        event.result(),
                        event.operation(),
                        DateUtil.date().getTime() / 1000,
                        Optional.ofNullable(DateUtil.date(event.sentAt())).map(DateTime::getTime).map(i -> i / 1000).orElse(0L)
                })
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentIO segmentIO(SegmentEventClientProperties properties,
                               SegmentEventReporter segmentEventReporter,
                               SegmentEventClientClickHouse<SegmentLogMessage> eventLogger,
                               List<ReactorMessageTransformer> transformers,
                               List<ReactorMessageInterceptor> interceptors) {
        return SegmentIO.builder()
                .reporter(segmentEventReporter)
                .eventLogger(eventLogger)
                .secret(properties.getSecret())
                .reportApp(properties.getAppId())
                .messageTransformers(transformers)
                .messageInterceptors(interceptors)
                .build();
    }
}
