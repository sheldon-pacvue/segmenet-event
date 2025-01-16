package com.pacvue.segment.event.springboot.properties.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Properties;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class ClickHouseProperties extends InstanceProperties {
    private Properties dataSourceProperties;
    private String insertSql;
}
