package com.pacvue.segment.event.springboot.properties.impl;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ReactorProperties {
    private int bufferMaxSize = 5;
    private int bufferTimeoutSeconds = 10;
}
