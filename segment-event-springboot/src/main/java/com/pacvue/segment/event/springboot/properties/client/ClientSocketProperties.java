package com.pacvue.segment.event.springboot.properties.client;

import com.pacvue.segment.event.springboot.properties.impl.SocketProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.client.ClientSocketProperties.PROPERTIES_PREFIX;

@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class ClientSocketProperties extends SocketProperties {
  public final static String PROPERTIES_PREFIX = "segment.event.client.socket";
}