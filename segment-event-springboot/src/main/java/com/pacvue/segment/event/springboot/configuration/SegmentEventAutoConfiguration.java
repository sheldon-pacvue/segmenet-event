package com.pacvue.segment.event.springboot.configuration;

import com.pacvue.segment.event.client.SegmentEventClient;
import com.pacvue.segment.event.client.SegmentEventClientAnalytics;
import com.pacvue.segment.event.client.SegmentEventClientRegistry;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.spring.client.SpringSegmentEventClientRegistry;
import com.segment.analytics.Analytics;
import com.segment.analytics.autoconfigure.SegmentAnalyticsAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
@ComponentScan(basePackages = "com.pacvue")
@ConfigurationPropertiesScan(basePackages = {
        "com.pacvue.segment.event.springboot.properties"
})
@ImportAutoConfiguration({
        SegmentAnalyticsAutoConfiguration.class
})
public class SegmentEventAutoConfiguration {
    @Bean
    @ConditionalOnProperty({"segment.analytics.writeKey"})
    @ConditionalOnMissingBean
    public SegmentEventClientAnalytics segmentEventClientAnalytics(Analytics segmentAnalytics) throws NoSuchFieldException, IllegalAccessException {
        return SegmentEventClientAnalytics.builder().analytics(segmentAnalytics).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentEventReporter segmentEventReporter(SegmentEventClientRegistry segmentEventClientRegistry) {
        return SegmentEventReporter.builder().registry(segmentEventClientRegistry).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentIO segmentIO(SegmentEventReporter segmentEventReporter) {
        return SegmentIO.builder()
                .reporter(segmentEventReporter)
                .build();
    }
}
