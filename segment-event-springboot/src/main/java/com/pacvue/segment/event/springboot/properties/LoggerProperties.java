package com.pacvue.segment.event.springboot.properties;

import com.pacvue.segment.event.springboot.properties.impl.*;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.LoggerProperties.PROPERTIES_PREFIX;


@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class LoggerProperties {
  public final static String PROPERTIES_PREFIX = "segment.event.logger";

  public ClickHouseProperties clickhouse;
  public FileProperties file;
  public HttpProperties http;
  public RabbitMQProperties rabbit;
  public SocketProperties socket;
}