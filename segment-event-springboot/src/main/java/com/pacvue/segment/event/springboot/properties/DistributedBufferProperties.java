package com.pacvue.segment.event.springboot.properties;

import com.pacvue.segment.event.springboot.properties.impl.ClickHouseProperties;
import com.pacvue.segment.event.springboot.properties.impl.RabbitMQProperties;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.pacvue.segment.event.springboot.properties.DistributedBufferProperties.PROPERTIES_PREFIX;

@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class DistributedBufferProperties {
    public final static String PROPERTIES_PREFIX = "segment.event.distributed";

    private RabbitMQProperties rabbit;
    private ClickHouseProperties clickhouse;
}
