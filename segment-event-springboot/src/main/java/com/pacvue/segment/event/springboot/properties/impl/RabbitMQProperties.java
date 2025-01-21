package com.pacvue.segment.event.springboot.properties.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class RabbitMQProperties extends BufferProperties {
    private String uri;
    private String exchangeName;
    private String routingKey;
    private String queueName;
}
