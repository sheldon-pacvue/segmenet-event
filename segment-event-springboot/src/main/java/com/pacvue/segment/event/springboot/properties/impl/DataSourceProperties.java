package com.pacvue.segment.event.springboot.properties.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Properties;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class DataSourceProperties extends BufferProperties {
    private String name = "dataSource";
    private String insertSql = """
                INSERT INTO SegmentEventLog (eventDate, hash, userId, type, message, result, operation, createdAt, eventTime)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
}
