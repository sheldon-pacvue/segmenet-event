package com.pacvue.segment.event.springboot.configuration;

import com.alibaba.druid.pool.DruidDataSource;
import com.pacvue.segment.event.client.SegmentEventClient;
import com.pacvue.segment.event.client.SegmentEventClientAnalytics;
import com.pacvue.segment.event.client.SegmentEventClientRegistry;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.entity.SegmentPersistingMessage;
import com.pacvue.segment.event.metric.MetricsCounter;
import com.pacvue.segment.event.spring.metrics.SpringPrometheusMetricsCounter;
import com.pacvue.segment.event.spring.client.SpringSegmentEventClientRegistry;
import com.pacvue.segment.event.springboot.properties.ClickHouseStoreProperties;
import com.pacvue.segment.event.springboot.properties.RabbitMQRemoteStoreProperties;
import com.pacvue.segment.event.springboot.properties.SegmentEventPrometheusMetricsProperties;
import com.pacvue.segment.event.store.ClickHouseStore;
import com.pacvue.segment.event.store.RabbitMQDistributedStore;
import com.pacvue.segment.event.store.Store;
import com.segment.analytics.Analytics;
import com.segment.analytics.autoconfigure.SegmentAnalyticsAutoConfiguration;
import com.segment.analytics.messages.Message;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeoutException;


@Configuration
@ComponentScan(basePackages = "com.pacvue")
@ConfigurationPropertiesScan(basePackages = {
        "com.pacvue.segment.event.springboot.properties"
})
@ImportAutoConfiguration({
        SegmentAnalyticsAutoConfiguration.class
})
public class SegmentEventAutoConfiguration {
    @Bean
    @ConditionalOnProperty({"segment.analytics.writeKey"})
    @ConditionalOnMissingBean
    public SegmentEventClientAnalytics segmentEventClientAnalytics(Analytics segmentAnalytics) throws NoSuchFieldException, IllegalAccessException {
        return SegmentEventClientAnalytics.builder().analytics(segmentAnalytics).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentEventClientRegistry segmentEventClientRegistry(List<? extends SegmentEventClient> clients) {
        return new SpringSegmentEventClientRegistry(clients);
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentEventReporter segmentEventReporter(SegmentEventClientRegistry segmentEventClientRegistry) {
        return SegmentEventReporter.builder().registry(segmentEventClientRegistry).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentIO segmentIO(SegmentEventReporter segmentEventReporter) {
        return SegmentIO.builder()
                .reporter(segmentEventReporter)
                .build().start();
    }
}
