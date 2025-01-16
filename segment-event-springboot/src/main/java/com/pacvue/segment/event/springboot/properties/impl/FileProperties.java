package com.pacvue.segment.event.springboot.properties.impl;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FileProperties {
    private String path;
    private String fileName = "analytics.log";
    // 默认文件大小为100Mb
    private long maxFileSizeMb = 100;
}
