package com.pacvue.segment.event.springboot.properties.impl;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Properties;

@Data
@Accessors(chain = true)
public class ClickHouseStoreProperties {
    private Properties dataSourceProperties;
    private String tableName = "SegmentEventsLog";
    private long loopIntervalMinutes = 1;
}
