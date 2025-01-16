package com.pacvue.segment.event.springboot.properties.client;

import com.pacvue.segment.event.springboot.properties.impl.ClickHouseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.client.ClientClickHouseProperties.PROPERTIES_PREFIX;

@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class ClientClickHouseProperties extends ClickHouseProperties {
  public final static String PROPERTIES_PREFIX = "segment.event.client.clickhouse";
}