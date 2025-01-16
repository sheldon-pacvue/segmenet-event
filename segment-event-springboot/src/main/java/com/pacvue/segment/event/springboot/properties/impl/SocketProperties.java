package com.pacvue.segment.event.springboot.properties.impl;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SocketProperties {
    private String host;
    private Integer port;
    private String endPoint;
}
