package com.pacvue.segment.event.springboot.properties.client;

import com.pacvue.segment.event.springboot.properties.impl.RabbitMQProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.client.ClientRabbitProperties.PROPERTIES_PREFIX;

@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class ClientRabbitProperties extends RabbitMQProperties {
  public final static String PROPERTIES_PREFIX = "segment.event.client.rabbit";
}