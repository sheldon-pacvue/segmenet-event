package com.pacvue.segment.event.springboot.properties.impl;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Properties;

@Data
@Accessors(chain = true)
public class ClickHouseProperties {
    private Properties dataSourceProperties;
    private String insertSql;
}
