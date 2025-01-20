package com.pacvue.segment.event.springboot.properties;

import com.pacvue.segment.event.springboot.properties.impl.*;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.ClientProperties.PROPERTIES_PREFIX;


@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class ClientProperties {
  public final static String PROPERTIES_PREFIX = "segment.event.client";

  public ClickHouseProperties clickhouse;
  public FileProperties file;
  public HttpProperties http;
  public RabbitMQProperties rabbit;
  public SocketProperties socket;
  public AmazonSQSProperties amazonSqs;
}