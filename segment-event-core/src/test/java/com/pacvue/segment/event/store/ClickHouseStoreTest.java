package com.pacvue.segment.event.store;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Slf4j
class ClickHouseStoreTest {
    private static ClickHouseStore store;

    @BeforeAll
    static void setUp() throws IOException {
        DruidDataSource dataSource = new DruidDataSource();
        Properties properties = new Properties();
        properties.setProperty("druid.driverClassName", "com.clickhouse.jdbc.ClickHouseDriver");
        properties.setProperty("druid.url", "jdbc:clickhouse://localhost:18123/default?clickhouse.jdbc.v2=true");
        properties.setProperty("druid.username", "default");
        properties.setProperty("druid.password", "default");
        properties.setProperty("druid.validationQuery", "SELECT 1");
        properties.setProperty("druid.testWhileIdle", "true");

        dataSource.configFromPropeties(properties);
        ClickHouseStore.builder()
                .dataSource(dataSource)
                .tableName("SegmentEventLog")
                .loopIntervalMinutes(1)
                .masterElection(new ZookeeperMasterElection("localhost:12181", "/segment/example"))
                .build()
                .createTableIfNotExists();
    }

    @Test
    void subscribe() throws InterruptedException {
        store.accept(events -> {
            log.info("events={}", events);
        });
        TimeUnit.MINUTES.sleep(10);
    }
}