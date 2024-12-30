package com.pacvue.segementeventexample.configuration;

import com.pacvue.segementeventexample.filter.RequestHolderFilter;
import com.pacvue.segment.event.client.SegmentEventClientFile;
import com.pacvue.segment.event.client.SegmentEventClientRegistry;
import com.pacvue.segment.event.core.SegmentEvent;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.holder.TtlContextHolder;
import com.pacvue.segment.event.springboot.configuration.SegmentEventAutoConfiguration;
import com.pacvue.segment.event.springboot.properties.SegmentEventClientFileProperties;
import com.pacvue.segment.event.store.RabbitMQDistributedStore;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportAutoConfiguration(SegmentEventAutoConfiguration.class)
public class ServerConfiguration {

    @Bean
    public TtlContextHolder<Integer> contextHolder() {
        return new TtlContextHolder<>();
    }

    @Bean
    public RequestHolderFilter requestHolderFilter() {
        return new RequestHolderFilter();
    }

    @Bean
    public SegmentEventClientFile segmentEventClientFile(SegmentEventClientFileProperties properties) {
        return new SegmentEventClientFile(properties.getPath(), properties.getFileName(), properties.getMaxFileSizeMb());
    }

    @Bean
    public SegmentEventReporter segmentEventReporter(SegmentEventClientRegistry segmentEventClientRegistry) {
        return SegmentEventReporter.builder().registry(segmentEventClientRegistry).defaultClientClass(SegmentEventClientFile.class).build();
    }


    @Bean
    public SegmentIO segmentIO(SegmentEventReporter segmentEventReporter, RabbitMQDistributedStore<SegmentEvent> distributedStore) {
        return SegmentIO.builder()
                .reporter(segmentEventReporter)
                .userIdContextHolder(contextHolder())
                .distributedStore(distributedStore).build().startSubscribe();
    }
}
