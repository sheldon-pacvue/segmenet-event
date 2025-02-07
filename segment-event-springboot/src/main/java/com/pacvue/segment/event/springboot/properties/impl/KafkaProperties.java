package com.pacvue.segment.event.springboot.properties.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Map;
import java.util.Properties;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class KafkaProperties extends BufferProperties {
    private String topic;
    private Properties properties;
}
