package com.pacvue.segment.event.example.configuration;

import com.pacvue.segment.event.client.*;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.entity.SegmentPersistingMessage;
import com.pacvue.segment.event.metric.MetricsCounter;
import com.pacvue.segment.event.spring.filter.ReactorRequestHolderFilter;
import com.pacvue.segment.event.spring.metrics.SpringPrometheusMetricsCounter;
import com.pacvue.segment.event.springboot.properties.*;
import com.pacvue.segment.event.springboot.properties.impl.RabbitMQRemoteStoreProperties;
import com.pacvue.segment.event.store.RabbitMQDistributedStore;
import com.pacvue.segment.event.store.Store;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.Message;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Configuration
public class ServerConfiguration {

    @Bean
    public ReactorRequestHolderFilter requestHolderFilter() {
        return new ReactorRequestHolderFilter();
    }

    @Bean
    public SegmentEventClientFile segmentEventClientFile(SegmentEventClientFileProperties properties) {
        return SegmentEventClientFile.builder()
                .path(properties.getPath())
                .fileName(properties.getFileName())
                .maxFileSizeMb(properties.getMaxFileSizeMb())
                .build();
    }

    @Bean
    public SegmentEventClientSocket segmentEventClientSocket(SegmentEventClientSocketProperties properties) {
        return SegmentEventClientSocket.builder()
                .host(properties.getHost())
                .port(properties.getPort())
                .secret(properties.getSecret())
                .endPoint(properties.getEndPoint())
                .build();
    }

    @Bean
    public SegmentEventClientHttp segmentEventClientHttp(SegmentEventClientHttpProperties properties) {
        // 设置 ConnectionProvider 配置
        ConnectionProvider provider = ConnectionProvider.builder("segment-event-client")
                .maxConnections(properties.getMaxConnections())  // 最大连接数
                .maxIdleTime(Duration.ofSeconds(properties.getMaxIdleTime()))  // 最大空闲时间
                .maxLifeTime(Duration.ofSeconds(properties.getMaxLifeTime()))  // 最大生命周期
                .pendingAcquireMaxCount(properties.getPendingAcquireMaxCount())  // 最大并发请求数
                .pendingAcquireTimeout(Duration.ofSeconds(properties.getPendingAcquireTimeout()))  // 获取连接的最大等待时间
                .build();

        // 创建 HttpClient 实例
        HttpClient httpClient = HttpClient.create(provider)
                .baseUrl(properties.getBaseUrl())  // 设置基础 URL
                .responseTimeout(Duration.ofSeconds(properties.getResponseTimeout()))  // 设置响应超时
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectionTimeout())  // 设置连接超时
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(properties.getReadTimeout(), TimeUnit.SECONDS))  // 设置读取超时
                        .addHandlerLast(new WriteTimeoutHandler(properties.getWriteTimeout(), TimeUnit.SECONDS)));  // 设置写入超时

        return SegmentEventClientHttp.builder()
                .httpClient(httpClient)
                .uri(properties.getUri())
                .method(properties.getMethod())
                .retry(properties.getRetry())
                .secret(properties.getSecret()).build();
    }

    @Bean
    @ConditionalOnBean(Analytics.class)
    public SegmentEventClientAnalytics segmentEventClientAnalytics(Analytics segmentAnalytics) throws NoSuchFieldException, IllegalAccessException {
        return SegmentEventClientAnalytics.builder().analytics(segmentAnalytics).build();
    }

    @Bean
    public MetricsCounter metricsCounter(MeterRegistry meterRegistry, SegmentEventPrometheusMetricsProperties properties) {
        return SpringPrometheusMetricsCounter.builder(meterRegistry, properties.getName())
                .tags(properties.getTags())
                .build();
    }

    @Bean
    public SegmentEventReporter segmentEventReporter(SegmentEventClientRegistry segmentEventClientRegistry, MetricsCounter metricsCounter) {
        return SegmentEventReporter.builder()
                .registry(segmentEventClientRegistry)
                .metricsCounter(metricsCounter)
                .build();
    }


    @Bean
    public Store<Message> distributedStore(DistributedStoreProperties<RabbitMQRemoteStoreProperties> properties) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        RabbitMQRemoteStoreProperties config = properties.getConfig();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(config.getUri());
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(config.getExchangeName(), BuiltinExchangeType.DIRECT, true, false, null);
        channel.queueDeclare(config.getQueueName(), true, false, false, null);
        channel.queueBind(config.getQueueName(), config.getExchangeName(), config.getRoutingKey());

        return RabbitMQDistributedStore.builder()
                .connection(connection)
                .channel(channel)
                .exchangeName(config.getExchangeName())
                .routingKey(config.getRoutingKey())
                .queueName(config.getQueueName())
                .build();
    }

    @Bean
    public SegmentIO segmentIO(SegmentEventReporter segmentEventReporter, Store<Message> distributedStore, Optional<Store<SegmentPersistingMessage>> persistingStore) {
        return SegmentIO.builder()
                .reporter(segmentEventReporter)
                .distributedStore(distributedStore)
                .persistingStore(persistingStore.orElse(null))
                .build();
    }
}
