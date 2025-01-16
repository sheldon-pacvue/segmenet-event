package com.pacvue.segment.event.springboot.configuration;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.pacvue.segment.event.client.SegmentEventClientAnalytics;
import com.pacvue.segment.event.client.SegmentEventClientClickHouse;
import com.pacvue.segment.event.client.SegmentEventClientRabbit;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.entity.SegmentLogMessage;
import com.pacvue.segment.event.springboot.properties.SegmentEventClientProperties;
import com.pacvue.segment.event.springboot.properties.client.ClientClickHouseProperties;
import com.pacvue.segment.event.springboot.properties.logger.LoggerRabbitProperties;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;


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

    @Bean
    public SegmentEventClientClickHouse<SegmentLogMessage> eventLogger(ClientClickHouseProperties properties) {
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.configFromPropeties(properties.getDataSourceProperties());

        return SegmentEventClientClickHouse.<SegmentLogMessage>builder()
                .dataSource(druidDataSource)
                .insertSql(properties.getInsertSql())
                .argumentsConverter(event -> new Object[]{
                        Optional.ofNullable(DateUtil.date(event.sentAt())).map(DateTime::toSqlDate).orElse(null),
                        DigestUtil.md5Hex(event.toString()),
                        event.userId(),
                        event.type().name(),
                        event.toString(),
                        event.result(),
                        event.operation(),
                        DateUtil.date().getTime(),
                        Optional.ofNullable(DateUtil.date(event.sentAt())).map(DateTime::getTime).orElse(0L)
                })
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentIO segmentIO(SegmentEventClientProperties properties, SegmentEventReporter segmentEventReporter, SegmentEventClientClickHouse<SegmentLogMessage> eventLogger) {
        return SegmentIO.builder()
                .reporter(segmentEventReporter)
                .eventLogger(eventLogger)
                .secret(properties.getSecret())
                .reportApp(properties.getAppId())
                .build();
    }
}
