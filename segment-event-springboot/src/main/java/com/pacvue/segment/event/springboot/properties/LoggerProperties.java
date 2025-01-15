package com.pacvue.segment.event.springboot.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.LoggerProperties.PROPERTIES_PREFIX;

@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class LoggerProperties<T> extends AbstractStoreProperties<T> {
    public final static String PROPERTIES_PREFIX = "segment.event.logger";
}
