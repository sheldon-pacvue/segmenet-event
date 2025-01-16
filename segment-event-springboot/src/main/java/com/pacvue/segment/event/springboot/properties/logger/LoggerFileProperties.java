package com.pacvue.segment.event.springboot.properties.logger;

import com.pacvue.segment.event.springboot.properties.impl.FileProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.logger.LoggerFileProperties.PROPERTIES_PREFIX;

@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class LoggerFileProperties extends FileProperties {
  public final static String PROPERTIES_PREFIX = "segment.event.logger.file";
}