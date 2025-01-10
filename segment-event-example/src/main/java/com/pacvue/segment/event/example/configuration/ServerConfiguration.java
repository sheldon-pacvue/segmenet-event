package com.pacvue.segment.event.example.configuration;

import com.alibaba.druid.pool.DruidDataSource;
import com.pacvue.segment.event.client.SegmentEventClientFile;
import com.pacvue.segment.event.client.SegmentEventClientRegistry;
import com.pacvue.segment.event.client.SegmentEventClientSocket;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.entity.SegmentPersistingMessage;
import com.pacvue.segment.event.metric.MetricsCounter;
import com.pacvue.segment.event.spring.filter.ReactorRequestHolderFilter;
import com.pacvue.segment.event.springboot.configuration.SegmentEventAutoConfiguration;
import com.pacvue.segment.event.springboot.properties.ClickHouseStoreProperties;
import com.pacvue.segment.event.springboot.properties.SegmentEventClientFileProperties;
import com.pacvue.segment.event.springboot.properties.SegmentEventClientSocketProperties;
import com.pacvue.segment.event.store.ClickHouseStore;
import com.pacvue.segment.event.store.Store;
import com.pacvue.segment.event.store.ZookeeperMasterElection;
import com.segment.analytics.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;


@Configuration
@ImportAutoConfiguration({
        SegmentEventAutoConfiguration.class,
})
public class ServerConfiguration {

    @Bean
    public ReactorRequestHolderFilter requestHolderFilter() {
        return new ReactorRequestHolderFilter();
    }

    @Bean
    public SegmentEventClientFile segmentEventClientFile(SegmentEventClientFileProperties properties) {
        return new SegmentEventClientFile(properties.getPath(), properties.getFileName(), properties.getMaxFileSizeMb());
    }

    @Bean
    public SegmentEventClientSocket segmentEventClientSocket(SegmentEventClientSocketProperties properties) {
        return new SegmentEventClientSocket(properties.getHost(), properties.getPort(), properties.getSecret(), properties.getEndPoint());
    }

    @Bean
    public SegmentEventReporter segmentEventReporter(SegmentEventClientRegistry segmentEventClientRegistry, MetricsCounter metricsCounter) {
        return SegmentEventReporter.builder().registry(segmentEventClientRegistry).metricsCounter(metricsCounter).defaultClientClass(SegmentEventClientSocket.class).build();
    }

    @Bean
    @ConditionalOnMissingBean
    @Qualifier("persistingStore")
    public Store<SegmentPersistingMessage> persistingStore(ClickHouseStoreProperties properties) throws IOException {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.configFromPropeties(properties.getDataSourceProperties());
        ClickHouseStore clickHouseStore = new ClickHouseStore(dataSource, properties.getTableName())
                .setMasterElection(new ZookeeperMasterElection("localhost:12181", "/segment/example"));
        clickHouseStore.createTableIfNotExists();
        return clickHouseStore;
    }


    @Bean
    public SegmentIO segmentIO(SegmentEventReporter segmentEventReporter,
                               @Qualifier("distributedStore") Store<Message> distributedStore,
                               @Qualifier("persistingStore") Store<SegmentPersistingMessage> persistingStore) {
        return SegmentIO.builder()
                .reporter(segmentEventReporter)
                .distributedStore(distributedStore)
                .persistingStore(persistingStore)
                .build().start();
    }
}
