package com.pacvue.segment.event.springboot.properties.logger;

import com.pacvue.segment.event.springboot.properties.impl.HttpProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.logger.LoggerHttpProperties.PROPERTIES_PREFIX;

@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class LoggerHttpProperties extends HttpProperties {
  public final static String PROPERTIES_PREFIX = "segment.event.logger.http";
}