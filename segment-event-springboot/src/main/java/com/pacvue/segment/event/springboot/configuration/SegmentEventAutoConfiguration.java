package com.pacvue.segment.event.springboot.configuration;

import com.pacvue.segment.event.client.SegmentEventClient;
import com.pacvue.segment.event.client.SegmentEventClientHttp;
import com.pacvue.segment.event.client.SegmentEventClientRegistry;
import com.pacvue.segment.event.generator.SegmentEvent;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.metric.MetricsCounter;
import com.pacvue.segment.event.metric.PrometheusMetricsCounter;
import com.pacvue.segment.event.spring.client.SpringSegmentEventClientRegistry;
import com.pacvue.segment.event.springboot.properties.RabbitMQRemoteStoreProperties;
import com.pacvue.segment.event.springboot.properties.SegmentEventClientHttpProperties;
import com.pacvue.segment.event.springboot.properties.SegmentEventPrometheusMetricsProperties;
import com.pacvue.segment.event.store.RabbitMQDistributedStore;
import com.pacvue.segment.event.store.Store;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import com.rabbitmq.client.*;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Configuration
@ConfigurationPropertiesScan(basePackages = {
        "com.pacvue.segment.event.springboot.properties"
})
public class SegmentEventAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public HttpClient httpClient(SegmentEventClientHttpProperties properties) {
        // 设置 ConnectionProvider 配置
        ConnectionProvider provider = ConnectionProvider.builder("segment-event-client")
                .maxConnections(properties.getMaxConnections())  // 最大连接数
                .maxIdleTime(Duration.ofSeconds(properties.getMaxIdleTime()))  // 最大空闲时间
                .maxLifeTime(Duration.ofSeconds(properties.getMaxLifeTime()))  // 最大生命周期
                .pendingAcquireMaxCount(properties.getPendingAcquireMaxCount())  // 最大并发请求数
                .pendingAcquireTimeout(Duration.ofSeconds(properties.getPendingAcquireTimeout()))  // 获取连接的最大等待时间
                .build();

        // 创建 HttpClient 实例
        return HttpClient.create(provider)
                .baseUrl(properties.getBaseUrl())  // 设置基础 URL
                .responseTimeout(Duration.ofSeconds(properties.getResponseTimeout()))  // 设置响应超时
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectionTimeout())  // 设置连接超时
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(properties.getReadTimeout(), TimeUnit.SECONDS))  // 设置读取超时
                        .addHandlerLast(new WriteTimeoutHandler(properties.getWriteTimeout(), TimeUnit.SECONDS)));  // 设置写入超时
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentEventClientHttp segmentEventClientHttp(HttpClient httpClient, SegmentEventClientHttpProperties properties) {
        return SegmentEventClientHttp.builder()
                .httpClient(httpClient)
                .uri(properties.getUri())
                .method(properties.getMethod())
                .retry(properties.getRetry())
                .secret(properties.getSecret()).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentEventClientRegistry segmentEventClientRegistry(List<? extends SegmentEventClient> clients) {
        return new SpringSegmentEventClientRegistry(clients);
    }

    @Bean
    @ConditionalOnMissingBean
    public MetricsCounter metricsCounter(SegmentEventPrometheusMetricsProperties properties) {
        return PrometheusMetricsCounter.builder(properties.getName())
                .subSystem(properties.getSubsystem())
                .namespace(properties.getNamespace())
                .help(properties.getHelp())
                .labelNames(properties.getLabels())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentEventReporter segmentEventReporter(SegmentEventClientRegistry segmentEventClientRegistry, MetricsCounter metricsCounter) {
        return SegmentEventReporter.builder().metricsCounter(metricsCounter).registry(segmentEventClientRegistry).build();
    }


    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = RabbitMQRemoteStoreProperties.PROPERTIES_PREFIX + ".enabled", havingValue = "true")
    public RabbitMQDistributedStore<SegmentEvent> distributedStore(RabbitMQRemoteStoreProperties properties) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(properties.getUri());
        return new RabbitMQDistributedStore<>(factory, properties.getExchangeName(), properties.getRoutingKey(), properties.getQueueName());
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentIO segmentIO(SegmentEventReporter segmentEventReporter, Store<SegmentEvent> distributedStore) {
        return SegmentIO.builder()
                .reporter(segmentEventReporter)
                .distributedStore(distributedStore)
                .build();
    }
}
