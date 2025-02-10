package com.pacvue.segment.event.springboot.properties;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.ReporterProperties.PROPERTIES_PREFIX;


@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class ReporterProperties {
  public final static String PROPERTIES_PREFIX = "segment.event.report";

  private int senderLimitCount = 20;
  private int loggerLimitCount = 20;
}