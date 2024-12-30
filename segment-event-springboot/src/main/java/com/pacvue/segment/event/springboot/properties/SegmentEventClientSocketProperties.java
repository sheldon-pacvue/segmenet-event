package com.pacvue.segment.event.springboot.properties;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.SegmentEventClientSocketProperties.PROPERTIES_PREFIX;

@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class SegmentEventClientSocketProperties {
    public final static String PROPERTIES_PREFIX = "segment.event.client.socket";

    private String host;
    private Integer port;
    private String protocol;
    private Integer timeout;
    private String endPoint;
}
