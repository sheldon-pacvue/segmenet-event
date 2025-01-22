package com.pacvue.segment.event.springboot.properties.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class AnalyticsProperties extends BufferProperties {
    private String writeKey;
}
