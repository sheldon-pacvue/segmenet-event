package com.pacvue.segment.event.example.configuration;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.pacvue.segment.event.client.*;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.entity.SegmentLogMessage;
import com.pacvue.segment.event.metric.MetricsCounter;
import com.pacvue.segment.event.spring.filter.ReactorRequestHolderFilter;
import com.pacvue.segment.event.spring.metrics.SpringPrometheusMetricsCounter;
import com.pacvue.segment.event.springboot.properties.*;
import com.pacvue.segment.event.springboot.properties.client.ClientClickHouseProperties;
import com.pacvue.segment.event.springboot.properties.client.ClientFileProperties;
import com.pacvue.segment.event.springboot.properties.client.ClientHttpProperties;
import com.pacvue.segment.event.springboot.properties.client.ClientSocketProperties;
import com.pacvue.segment.event.springboot.properties.impl.*;
import com.pacvue.segment.event.buffer.RabbitMQDistributedBuffer;
import com.pacvue.segment.event.buffer.Buffer;
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
    public SegmentEventClientFile<Message> segmentEventClientFile(ClientFileProperties properties) {
        return SegmentEventClientFile.builder()
                .path(properties.getPath())
                .fileName(properties.getFileName())
                .maxFileSizeMb(properties.getMaxFileSizeMb())
                .build();
    }

    @Bean
    public SegmentEventClientSocket<Message> segmentEventClientSocket(SegmentEventClientProperties common, ClientSocketProperties properties) {
        return SegmentEventClientSocket.builder()
                .host(properties.getHost())
                .port(properties.getPort())
                .secret(common.getSecret())
                .endPoint(properties.getEndPoint())
                .build();
    }

    @Bean
    public SegmentEventClientHttp<Message> segmentEventClientHttp(ClientHttpProperties properties) {
        // 设置 ConnectionProvider 配置
        ConnectionProvider provider = ConnectionProvider.builder(properties.getThreadName())
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
    public MetricsCounter metricsCounter(MeterRegistry meterRegistry, PrometheusMetricsProperties properties) {
        return SpringPrometheusMetricsCounter.builder(meterRegistry, properties.getName())
                .tags(properties.getTags())
                .build();
    }

    @Bean
    public SegmentEventReporter segmentEventReporter(SegmentEventClientSocket<Message> client, MetricsCounter metricsCounter) {
        return SegmentEventReporter.builder()
                .client(client)
                .metricsCounter(metricsCounter)
                .build();
    }


    @Bean
    public Buffer<Message> distributedStore(DistributedObjectProperties<RabbitMQProperties> properties) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        RabbitMQProperties config = properties.getConfig();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(config.getUri());
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(config.getExchangeName(), BuiltinExchangeType.DIRECT, true, false, null);
        channel.queueDeclare(config.getQueueName(), true, false, false, null);
        channel.queueBind(config.getQueueName(), config.getExchangeName(), config.getRoutingKey());

        return RabbitMQDistributedBuffer.builder()
                .connection(connection)
                .channel(channel)
                .exchangeName(config.getExchangeName())
                .routingKey(config.getRoutingKey())
                .queueName(config.getQueueName())
                .build();
    }

    @Bean
    public SegmentIO segmentIO(SegmentEventClientProperties properties, SegmentEventReporter segmentEventReporter, Buffer<Message> distributedBuffer, SegmentEventClientClickHouse<SegmentLogMessage> eventLogger) {
        return SegmentIO.builder()
                .reporter(segmentEventReporter)
                .distributedBuffer(distributedBuffer)
                .eventLogger(eventLogger)
                .secret(properties.getSecret())
                .reportApp(properties.getAppId())
                .build();
    }
}
