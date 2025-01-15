package com.pacvue.segment.event.springboot.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.PersistingStoreProperties.PROPERTIES_PREFIX;

@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class PersistingStoreProperties<T> extends AbstractStoreProperties<T> {
    public final static String PROPERTIES_PREFIX = "segment.event.store.persisting";
}
