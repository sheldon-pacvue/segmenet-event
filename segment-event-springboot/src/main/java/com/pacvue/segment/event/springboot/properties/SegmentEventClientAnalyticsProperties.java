package com.pacvue.segment.event.springboot.properties;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.SegmentEventClientAnalyticsProperties.PROPERTIES_PREFIX;

@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class SegmentEventClientAnalyticsProperties {
  public final static String PROPERTIES_PREFIX = "segment.event.client.analytics";

  private String secret;
}