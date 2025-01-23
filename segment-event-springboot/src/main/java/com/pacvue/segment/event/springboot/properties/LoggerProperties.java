package com.pacvue.segment.event.springboot.properties;

import com.pacvue.segment.event.springboot.properties.impl.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.LoggerProperties.PROPERTIES_PREFIX;


@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class LoggerProperties extends BufferProperties {
  public final static String PROPERTIES_PREFIX = "segment.event.logger";

  public DataSourceProperties datasource;
  public FileProperties file;
  public HttpProperties http;
  public RabbitMQProperties rabbit;
  public SocketProperties socket;
  public AmazonSQSProperties amazonSqs;
  public AnalyticsProperties analytics;
}