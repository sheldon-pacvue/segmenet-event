package com.pacvue.segment.event.springboot.properties.impl;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AmazonSQSProperties {
    private String region;
    private String queueUrl;
    private String awsAccessKey;
    private String awsSecretKey;
}
