package com.pacvue.segment.event.springboot.properties.logger;

import com.pacvue.segment.event.springboot.properties.impl.RabbitMQProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.logger.LoggerRabbitProperties.PROPERTIES_PREFIX;

@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class LoggerRabbitProperties extends RabbitMQProperties {
  public final static String PROPERTIES_PREFIX = "segment.event.logger.rabbit";
}