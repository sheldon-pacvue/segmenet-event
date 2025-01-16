package com.pacvue.segment.event.springboot.properties.logger;

import com.pacvue.segment.event.springboot.properties.impl.ClickHouseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.logger.LoggerClickHouseProperties.PROPERTIES_PREFIX;

@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class LoggerClickHouseProperties extends ClickHouseProperties {
  public final static String PROPERTIES_PREFIX = "segment.event.logger.clickhouse";
}