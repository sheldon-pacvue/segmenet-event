package com.pacvue.segementeventexample.configuration;

import com.pacvue.segementeventexample.filter.RequestHolderFilter;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.holder.TtlContextHolder;
import com.pacvue.segment.event.springboot.configuration.SegmentEventAutoConfiguration;
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
    public SegmentIO segmentIO(SegmentEventReporter segmentEventReporter) {
        return SegmentIO.builder().reporter(segmentEventReporter).userIdContextHolder(contextHolder()).build().startSubscribe();
    }
}
