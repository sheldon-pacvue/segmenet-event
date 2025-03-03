package com.pacvue.segment.event.springboot.properties.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class SocketProperties extends BufferProperties {
    private String host;
    private Integer port;
    private String endPoint;
    private String authorization;
}
