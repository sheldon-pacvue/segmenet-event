package com.pacvue.segment.event.springboot.properties;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.pacvue.segment.event.springboot.properties.RabbitMQRemoteStoreProperties.PROPERTIES_PREFIX;

@Data
@Accessors(chain = true)
@Configuration
@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class RabbitMQRemoteStoreProperties {
    public final static String PROPERTIES_PREFIX = "segment.event.rabbit.store";

    private boolean enabled;
    private String url;
    private String exchangeName;
    private String routingKey;
    private String queueName;
    private int consumeCount = 5;
}
