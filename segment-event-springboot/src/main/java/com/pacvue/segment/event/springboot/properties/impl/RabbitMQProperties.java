package com.pacvue.segment.event.springboot.properties.impl;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RabbitMQProperties {
    private String uri;
    private String exchangeName;
    private String routingKey;
    private String queueName;
}
