package com.pacvue.segementeventexample.controller;

import com.pacvue.segment.event.client.SegmentEventClientHttp;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.entity.SegmentEventTrace;
import com.pacvue.segment.event.generator.SegmentEventGenerator;
import com.pacvue.segment.event.holder.TtlContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
public class ExampleController {
    private static final Logger log = LoggerFactory.getLogger(ExampleController.class);

    @Autowired
    private TtlContextHolder<Integer> contextHolder;

    @Autowired
    private SegmentIO segmentIO;

    @GetMapping("/503")
    public Mono<Integer> func503() throws InterruptedException {
        Thread.sleep(7000);
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable");
    }


    @GetMapping("/hello")
    public Mono<Integer> hello() {
        Integer context = contextHolder.getContext();
        log.info("controller: {}", contextHolder.getContext());
        segmentIO.trace(clazz -> Mono.just(new SegmentEventTrace()));
        log.info("context2: {}", context);
        return Mono.just(context);
    }

    @PostMapping("/v1/import")
    public Mono<Integer> v1Import(@RequestBody SegmentEventClientHttp.Body body) {
        log.info("import: {}", body);
        // 假设你在某些条件下想抛出 500 错误
        if (body.getBatch() == null || body.getBatch().size() > 4) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Batch is too big");
        }
        return Mono.just(body.getBatch().size());
    }
}
