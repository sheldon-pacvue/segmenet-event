package com.pacvue.segment.event.springboot.properties.logger;

import com.pacvue.segment.event.springboot.properties.impl.SocketProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.logger.LoggerSocketProperties.PROPERTIES_PREFIX;

@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class LoggerSocketProperties extends SocketProperties {
  public final static String PROPERTIES_PREFIX = "segment.event.logger.socket";
}