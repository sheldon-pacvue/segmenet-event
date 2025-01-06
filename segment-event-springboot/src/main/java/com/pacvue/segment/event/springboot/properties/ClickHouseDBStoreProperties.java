package com.pacvue.segment.event.springboot.properties;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Properties;

import static com.pacvue.segment.event.springboot.properties.ClickHouseDBStoreProperties.PROPERTIES_PREFIX;

@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class ClickHouseDBStoreProperties {
    public final static String PROPERTIES_PREFIX = "segment.event.clickhouse.store";

    private boolean enabled;
    private Properties dataSourceProperties;
    private String tableName = "SegmentEventsLog";
}
