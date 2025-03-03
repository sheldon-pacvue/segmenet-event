package com.pacvue.segment.event.springboot.properties;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.PrometheusMetricsProperties.PROPERTIES_PREFIX;

@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class PrometheusMetricsProperties {
    public final static String PROPERTIES_PREFIX = "segment.event.metrics.prometheus";

    private String name = "helium10_segment_async_send_events_total";
    private String[] tags = new String[]{};
}
