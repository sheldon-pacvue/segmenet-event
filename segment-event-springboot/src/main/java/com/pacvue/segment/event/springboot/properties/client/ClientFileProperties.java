package com.pacvue.segment.event.springboot.properties.client;

import com.pacvue.segment.event.springboot.properties.impl.FileProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.client.ClientFileProperties.PROPERTIES_PREFIX;

@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class ClientFileProperties extends FileProperties {
  public final static String PROPERTIES_PREFIX = "segment.event.client.file";
}