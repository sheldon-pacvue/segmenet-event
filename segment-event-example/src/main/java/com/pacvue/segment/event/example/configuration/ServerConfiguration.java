package com.pacvue.segment.event.example.configuration;

import com.pacvue.segment.event.client.*;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.entity.SegmentLogMessage;
import com.pacvue.segment.event.metric.MetricsCounter;
import com.pacvue.segment.event.spring.filter.ReactorRequestHolderFilter;
import com.pacvue.segment.event.spring.metrics.SpringPrometheusMetricsCounter;
import com.pacvue.segment.event.spring.transformer.UserIdReactorMessageTransformer;
import com.pacvue.segment.event.springboot.properties.*;
import com.pacvue.segment.event.springboot.properties.impl.*;
import com.pacvue.segment.event.buffer.RabbitMQDistributedBuffer;
import com.pacvue.segment.event.buffer.Buffer;
import com.pacvue.segment.event.extend.ReactorMessageTransformer;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.segment.analytics.messages.Message;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Configuration
public class ServerConfiguration {

    @Bean
    public ReactorRequestHolderFilter requestHolderFilter() {
        return new ReactorRequestHolderFilter();
    }

    @Bean
    public SegmentEventClientFile<Message> segmentEventClientFile(ClientProperties properties) {
        FileProperties file = properties.getFile();
        return SegmentEventClientFile.builder()
                .path(file.getPath())
                .fileName(file.getFileName())
                .maxFileSizeMb(file.getMaxFileSizeMb())
                .build();
    }

    @Bean
    public SegmentEventClientSocket<Message> segmentEventClientSocket(SegmentEventClientProperties common, ClientProperties properties) {
        SocketProperties socket = properties.getSocket();
        return SegmentEventClientSocket.builder()
                .host(socket.getHost())
                .port(socket.getPort())
                .secret(common.getSecret())
                .endPoint(socket.getEndPoint())
                .build();
    }

    @Bean
    public SegmentEventClientHttp<Message> segmentEventClientHttp(ClientProperties properties, SegmentEventClientProperties common) {
        HttpProperties http = properties.getHttp();
        // 设置 ConnectionProvider 配置
        ConnectionProvider provider = ConnectionProvider.builder(http.getThreadName())
                .maxConnections(http.getMaxConnections())  // 最大连接数
                .maxIdleTime(Duration.ofSeconds(http.getMaxIdleTime()))  // 最大空闲时间
                .maxLifeTime(Duration.ofSeconds(http.getMaxLifeTime()))  // 最大生命周期
                .pendingAcquireMaxCount(http.getPendingAcquireMaxCount())  // 最大并发请求数
                .pendingAcquireTimeout(Duration.ofSeconds(http.getPendingAcquireTimeout()))  // 获取连接的最大等待时间
                .build();

        // 创建 HttpClient 实例
        HttpClient httpClient = HttpClient.create(provider)
                .baseUrl(http.getBaseUrl())  // 设置基础 URL
                .responseTimeout(Duration.ofSeconds(http.getResponseTimeout()))  // 设置响应超时
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, http.getConnectionTimeout())  // 设置连接超时
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(http.getReadTimeout(), TimeUnit.SECONDS))  // 设置读取超时
                        .addHandlerLast(new WriteTimeoutHandler(http.getWriteTimeout(), TimeUnit.SECONDS)));  // 设置写入超时

        return SegmentEventClientHttp.builder()
                .httpClient(httpClient)
                .uri(http.getUri())
                .method(http.getMethod())
                .retry(http.getRetry())
                .secret(common.getSecret())
                .build();
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
    public Buffer<Message> distributedStore(DistributedBufferProperties properties) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        RabbitMQProperties config = properties.getRabbit();

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
    public UserIdReactorMessageTransformer userIdReactorMessageTransformer() {
        return new UserIdReactorMessageTransformer("X-User-ID");
    }

    @Bean
    public SegmentIO segmentIO(SegmentEventClientProperties properties,
                               SegmentEventReporter segmentEventReporter,
                               SegmentEventClientClickHouse<SegmentLogMessage> eventLogger,
                               List<ReactorMessageTransformer> transformers) {
        return SegmentIO.builder()
                .reporter(segmentEventReporter)
                .eventLogger(eventLogger)
                .secret(properties.getSecret())
                .reportApp(properties.getAppId())
                .messageTransformers(transformers)
                .build();
    }
}
