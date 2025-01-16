package com.pacvue.segment.event.springboot.properties.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class FileProperties extends InstanceProperties {
    private String path;
    private String fileName;
    // 默认文件大小为100Mb
    private long maxFileSizeMb = 100;
}
