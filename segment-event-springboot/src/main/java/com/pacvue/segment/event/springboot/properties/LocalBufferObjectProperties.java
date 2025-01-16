package com.pacvue.segment.event.springboot.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.LocalBufferObjectProperties.PROPERTIES_PREFIX;

@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class LocalBufferObjectProperties<T> extends AbstractObjectProperties<T> {
    public final static String PROPERTIES_PREFIX = "segment.event.store.local";
}
