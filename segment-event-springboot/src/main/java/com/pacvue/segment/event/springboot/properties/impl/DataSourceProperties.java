package com.pacvue.segment.event.springboot.properties.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Properties;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class DataSourceProperties extends BufferProperties {
    private final static Properties DEFAULT_DATASOURCE;

    static {
        DEFAULT_DATASOURCE = new Properties();
        DEFAULT_DATASOURCE.setProperty("druid.driver-class-name", "com.clickhouse.jdbc.ClickHouseDriver");
        DEFAULT_DATASOURCE.setProperty("druid.validationQuery", "SELECT 1");
        DEFAULT_DATASOURCE.setProperty("druid.testWhileIdle", "true");
    }

    private Properties dataSourceProperties = DEFAULT_DATASOURCE;
    private String insertSql = """
                INSERT INTO SegmentEventLog (eventDate, hash, userId, type, message, result, operation, createdAt, eventTime)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
}
