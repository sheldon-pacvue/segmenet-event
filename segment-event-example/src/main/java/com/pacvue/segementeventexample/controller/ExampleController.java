package com.pacvue.segementeventexample.controller;

import com.pacvue.segment.event.holder.TtlContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class ExampleController {
    private static final Logger log = LoggerFactory.getLogger(ExampleController.class);

    @Autowired
    private TtlContextHolder<String> contextHolder;

    @GetMapping("/hello")
    public Mono<String> hello() {
        String context = contextHolder.getContext();
        log.info("controller: {}", contextHolder.getContext());
        return Mono.just(context);
    }
}
