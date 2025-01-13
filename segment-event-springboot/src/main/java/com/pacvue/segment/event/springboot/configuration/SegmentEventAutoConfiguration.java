package com.pacvue.segment.event.springboot.configuration;

import com.pacvue.segment.event.client.SegmentEventClientAnalytics;
import com.pacvue.segment.event.client.SegmentEventClientRegistry;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.springboot.properties.SegmentEventClientAnalyticsProperties;
import com.segment.analytics.Analytics;
import com.segment.analytics.Log;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ComponentScan(basePackages = "com.pacvue")
@ConfigurationPropertiesScan(basePackages = {
        "com.pacvue.segment.event.springboot.properties"
})
public class SegmentEventAutoConfiguration {
    @Bean
    @ConditionalOnProperty(SegmentEventClientAnalyticsProperties.PROPERTIES_PREFIX + ".secret")
    @ConditionalOnMissingBean
    public Analytics segmentAnalytics(SegmentEventClientAnalyticsProperties properties) {
        Logger log = LoggerFactory.getLogger(Analytics.class);
        return Analytics.builder(properties.getSecret()).log(new Log() {
            @Override
            public void print(Level level, String format, Object... args) {
                log.debug(String.format(format, args));
            }

            @Override
            public void print(Level level, Throwable error, String format, Object... args) {
                log.error(String.format(format, args), error);
            }
        }).build();
    }

    @Bean
    @ConditionalOnBean(Analytics.class)
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
