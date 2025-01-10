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
    public MetricsCounter metricsCounter(MeterRegistry meterRegistry, SegmentEventPrometheusMetricsProperties properties) {
        return SpringPrometheusMetricsCounter.builder(meterRegistry, properties.getName())
                .tags(properties.getTags())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentEventReporter segmentEventReporter(SegmentEventClientRegistry segmentEventClientRegistry, MetricsCounter metricsCounter) {
        return SegmentEventReporter.builder().metricsCounter(metricsCounter).registry(segmentEventClientRegistry).build();
    }


    @Bean
    @ConditionalOnMissingBean(name = "distributedStore")
    @ConditionalOnProperty(value = RabbitMQRemoteStoreProperties.PROPERTIES_PREFIX + ".enabled", havingValue = "true")
    public Store<Message> distributedStore(RabbitMQRemoteStoreProperties properties) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(properties.getUri());
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(properties.getExchangeName(), BuiltinExchangeType.DIRECT, true, false, null);
        channel.queueDeclare(properties.getQueueName(), true, false, false, null);
        channel.queueBind(properties.getQueueName(), properties.getExchangeName(), properties.getRoutingKey());

        return RabbitMQDistributedStore.builder()
                .connection(connection)
                .channel(channel)
                .exchangeName(properties.getExchangeName())
                .routingKey(properties.getRoutingKey())
                .queueName(properties.getQueueName())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "persistingStore")
    @Qualifier("persistingStore")
    public Store<SegmentPersistingMessage> persistingStore(ClickHouseStoreProperties properties) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.configFromPropeties(properties.getDataSourceProperties());
        ClickHouseStore clickHouseStore = new ClickHouseStore(dataSource, properties.getTableName());
        clickHouseStore.createTableIfNotExists();
        return clickHouseStore;
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentIO segmentIO(SegmentEventReporter segmentEventReporter,
                               @Qualifier("distributedStore") Store<Message> distributedStore,
                               @Qualifier("persistingStore") Store<SegmentPersistingMessage> persistingStore) {
        return SegmentIO.builder()
                .reporter(segmentEventReporter)
                .distributedStore(distributedStore)
                .persistingStore(persistingStore)
                .build();
    }
}
