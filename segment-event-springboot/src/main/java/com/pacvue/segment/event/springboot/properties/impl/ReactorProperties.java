package com.pacvue.segment.event.springboot.properties.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class ReactorProperties extends InstanceProperties {
    private int bufferMaxSize = 5;
    private int bufferTimeoutSeconds = 10;
}
