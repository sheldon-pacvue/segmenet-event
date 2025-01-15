package com.pacvue.segment.event.store;

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
class RabbitMQDistributedStoreTest {
    private final static String URI = "amqp://deploy:deploy@localhost:5672";
    private final static String EXCHANGE = "amq.direct";
    private final static String ROUTING_KEY = "segment-event-example";
    private final static String QUEUE_NAME = "segment-event-example";
    private static RabbitMQDistributedStore<Message> store;

    @BeforeAll
    static void setUp() throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(URI);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.DIRECT, true, false, null);
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        channel.queueBind(QUEUE_NAME, EXCHANGE, ROUTING_KEY);

        store = RabbitMQDistributedStore.builder()
                .connection(connection)
                .channel(channel)
                .exchangeName(EXCHANGE)
                .routingKey(ROUTING_KEY)
                .queueName(QUEUE_NAME)
                .build();
    }

    @AfterAll
    static void tearDown() throws IOException, TimeoutException {
        store.shutdown();
    }

    @Test
    @Order(1)
    void commit() {
        Boolean result = store.commit(TrackMessage.builder("123").sentAt(new Date()).anonymousId("123").userId("123").build()).block();
        assertEquals(true, result);
    }

    @Test
    @Order(2)
    void accept() throws InterruptedException, IOException {
        StopAccept stopAccept = store.accept(events -> {
            System.out.println(events);
            assertEquals(1, events.size());
        });

        // Allow time for the callback to execute
        Thread.sleep(1000);

        // Stop accepting messages
        stopAccept.stop();
    }

    @Test
    @Order(3)
    void acceptAgain() {
        store.accept(events -> {
            System.out.println(events);
            assertEquals(1, events.size());
        });
        assertThrows(IllegalStateException.class, () -> store.accept(events -> {
            System.out.println(events);
            assertEquals(1, events.size());
        }));
    }

    @Test
    @Order(4)
    void shutdownAndCommit() {
        store.shutdown();
        assertThrows(RuntimeException.class, () -> store.commit(TrackMessage.builder("123").sentAt(new Date()).anonymousId("123").userId("123").build()).block());
    }
}