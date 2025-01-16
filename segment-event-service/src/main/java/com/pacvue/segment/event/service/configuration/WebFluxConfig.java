package com.pacvue.segment.event.service.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.PathMatchConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    @Override
    public void configurePathMatching(PathMatchConfigurer configurer) {
        // 这里设置前缀
        configurer.addPathPrefix("/segment", c -> true);  // 这会将所有的路径加上 "/api" 前缀
    }
}