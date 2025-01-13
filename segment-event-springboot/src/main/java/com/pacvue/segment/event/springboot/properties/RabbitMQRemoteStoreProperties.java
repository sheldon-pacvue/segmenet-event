package com.pacvue.segment.event.springboot.properties;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.RabbitMQRemoteStoreProperties.PROPERTIES_PREFIX;

@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class RabbitMQRemoteStoreProperties {
    public final static String PROPERTIES_PREFIX = "segment.event.store.rabbit";

    private String uri;
    private String exchangeName;
    private String routingKey;
    private String queueName;
}
