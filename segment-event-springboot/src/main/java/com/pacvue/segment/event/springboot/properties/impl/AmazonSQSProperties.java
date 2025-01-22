package com.pacvue.segment.event.springboot.properties.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class AmazonSQSProperties extends BufferProperties {
    private String region;
    private String queueName;
    private String awsAccessKey;
    private String awsSecretKey;
}
