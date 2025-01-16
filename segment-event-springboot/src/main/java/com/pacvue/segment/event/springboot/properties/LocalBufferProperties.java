package com.pacvue.segment.event.springboot.properties;

import com.pacvue.segment.event.springboot.properties.impl.ReactorProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.LocalBufferProperties.PROPERTIES_PREFIX;

@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class LocalBufferProperties {
    public final static String PROPERTIES_PREFIX = "segment.event.local";

    private ReactorProperties reactor;
}
