package com.pacvue.segment.event.springboot.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.LoggerObjectProperties.PROPERTIES_PREFIX;

@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class LoggerObjectProperties<T> extends AbstractObjectProperties<T> {
    public final static String PROPERTIES_PREFIX = "segment.event.logger";
}
