package com.pacvue.segment.event.springboot.properties;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.SegmentEventPrometheusMetricsProperties.PROPERTIES_PREFIX;

@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class SegmentEventPrometheusMetricsProperties {
    public final static String PROPERTIES_PREFIX = "segment.event.prometheus.metrics";

    private String name = "helium10_segment_async_send_events_total";
    private String namespace = "namespace";
    private String subsystem = "subsystem";
    private String help = "total number of segment event reported";
    private String[] labels = new String[]{};
}
