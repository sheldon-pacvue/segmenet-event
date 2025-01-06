package com.pacvue.segementeventexample.configuration;

import com.pacvue.segment.event.client.SegmentEventClientFile;
import com.pacvue.segment.event.client.SegmentEventClientRegistry;
import com.pacvue.segment.event.client.SegmentEventClientSocket;
import com.pacvue.segment.event.entity.SegmentEventOptional;
import com.pacvue.segment.event.generator.SegmentEvent;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.metric.MetricsCounter;
import com.pacvue.segment.event.spring.filter.ReactorRequestHolderFilter;
import com.pacvue.segment.event.springboot.configuration.SegmentEventAutoConfiguration;
import com.pacvue.segment.event.springboot.properties.SegmentEventClientFileProperties;
import com.pacvue.segment.event.springboot.properties.SegmentEventClientSocketProperties;
import com.pacvue.segment.event.store.Store;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ImportAutoConfiguration(SegmentEventAutoConfiguration.class)
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
    public SegmentIO segmentIO(SegmentEventReporter segmentEventReporter,
                               @Qualifier("distributedStore") Store<Void> distributedStore,
                               @Qualifier("dbStore") Store<SegmentEventOptional> dbStore) {
        return SegmentIO.builder()
                .reporter(segmentEventReporter)
                .distributedStore(distributedStore)
                .dbStore(dbStore)
                .build().start();
    }
}
