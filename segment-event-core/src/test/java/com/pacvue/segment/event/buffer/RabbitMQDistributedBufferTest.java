package com.pacvue.segment.event.buffer;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.segment.analytics.messages.Message;
import com.segment.analytics.messages.TrackMessage;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // 按顺序执行测试方法
class RabbitMQDistributedBufferTest {
    private final static String URI = "amqp://deploy:deploy@localhost:5678";
    private final static String EXCHANGE = "amq.direct";
    private final static String ROUTING_KEY = "segment-event-example";
    private final static String QUEUE_NAME = "segment-event-example";

}