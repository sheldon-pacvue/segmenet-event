package com.pacvue.segment.event.example.controller;

import com.pacvue.segment.event.client.SegmentEventClientHttp;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.example.generator.TrackSimpleGenerator;
import com.segment.analytics.messages.Message;
import com.segment.analytics.messages.TrackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;

@RestController
public class ExampleController {
    private static final Logger log = LoggerFactory.getLogger(ExampleController.class);

    @Autowired
    private SegmentIO segmentIO;

    @Autowired
    private TrackSimpleGenerator trackSimpleGenerator;

    @GetMapping("/503")
    public Mono<Integer> func503() throws InterruptedException {
        Thread.sleep(7000);
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable");
    }

    /**
     * Right
     *
     * @return
     */
    @GetMapping("/hello/right")
    public Mono<String> right() {
        return Mono.deferContextual(ctx -> {
            ServerWebExchange context = ctx.get(ServerWebExchange.class);
            segmentIO.track(() -> {
                String userId = context.getRequest().getHeaders().getFirst("X-User-ID");
                log.info("userId: {}", userId);
                return Mono.just(TrackMessage.builder("hello-right").userId(userId).anonymousId(userId).sentAt(new Date()));
            });
            return Mono.just("right");
        });
    }

    /**
     * Right
     *
     * @return
     */
    @GetMapping("/hello/right2")
    public Mono<String> right2() {
        return Mono.defer(() -> segmentIO.deliverReact(() -> Mono.deferContextual(ctx -> {
            ServerWebExchange context = ctx.get(ServerWebExchange.class);
            String userId = context.getRequest().getHeaders().getFirst("X-User-ID");
            log.info("userId: {}", userId);
            return Mono.just(TrackMessage.builder("hello-right2").userId(userId).anonymousId(userId).sentAt(new Date()));
        })))
        .flatMap(b -> Mono.just(b.toString()));
    }

    /**
     * Right
     *
     * @return
     */
    @GetMapping("/hello/right3")
    public Mono<String> right3() {
        segmentIO.track(trackSimpleGenerator);
        return Mono.just("right3");
    }


    /**
     * Wrong
     *
     * @return
     */
    @GetMapping("/hello/wrong")
    public Mono<String> wrong() {
        segmentIO.track(() -> Mono.deferContextual((ctx) -> {
            ServerWebExchange context = ctx.getOrDefault(ServerWebExchange.class, null);
            String userId = context.getRequest().getHeaders().getFirst("X-User-ID");
            log.info("userId: {}", userId);
            return Mono.just(TrackMessage.builder("hello-wrong").userId(userId));
        }));
        return Mono.just("wrong");
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

    @PostMapping("/v1/segment/event/report")
    public Mono<Integer> v1Import(@RequestBody Message message) {
        log.info("import: {}", message);

        return Mono.just(1);
    }
}
