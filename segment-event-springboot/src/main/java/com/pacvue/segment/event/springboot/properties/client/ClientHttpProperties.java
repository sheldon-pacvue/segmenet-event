package com.pacvue.segment.event.springboot.properties.client;

import com.pacvue.segment.event.springboot.properties.impl.HttpProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.client.ClientHttpProperties.PROPERTIES_PREFIX;

@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class ClientHttpProperties extends HttpProperties {
  public final static String PROPERTIES_PREFIX = "segment.event.client.http";
}