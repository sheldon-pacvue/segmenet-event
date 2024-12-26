package com.pacvue.segementeventexample.configuration;

import com.pacvue.segementeventexample.filter.RequestHolderFilter;
import com.pacvue.segment.event.holder.TtlContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerConfiguration {

    @Bean
    public TtlContextHolder<String> contextHolder() {
        return new TtlContextHolder<>();
    }

    @Bean
    public RequestHolderFilter requestHolderFilter() {
        return new RequestHolderFilter();
    }
}
