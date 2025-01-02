package com.pacvue.segementeventexample.controller;

import com.pacvue.segment.event.client.SegmentEventClientHttp;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.entity.SegmentEventTrace;
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

@RestController
public class ExampleController {
    private static final Logger log = LoggerFactory.getLogger(ExampleController.class);

    @Autowired
    private SegmentIO segmentIO;

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
            segmentIO.trace(clazz -> Mono.defer(() -> {
                String userId = context.getRequest().getHeaders().getFirst("X-User-ID");
                log.info("userId: {}", userId);

                SegmentEventTrace segmentEventTrace = new SegmentEventTrace();
                segmentEventTrace.setUserId(Integer.parseInt(userId));
                return Mono.just(segmentEventTrace);
            }));
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
        return Mono.defer(() -> segmentIO.deliverReact(clazz -> Mono.deferContextual(ctx -> {
            ServerWebExchange context = ctx.get(ServerWebExchange.class);
            String userId = context.getRequest().getHeaders().getFirst("X-User-ID");
            log.info("userId: {}", userId);

            SegmentEventTrace segmentEventTrace = new SegmentEventTrace();
            segmentEventTrace.setUserId(Integer.parseInt(userId));

            return Mono.just(segmentEventTrace);
        }), SegmentEventTrace.class))
                .flatMap(b -> Mono.just(b.toString()));
    }


    /**
     * Wrong
     *
     * @return
     */
    @GetMapping("/hello/wrong")
    public Mono<String> wrong() {
        segmentIO.trace(clazz -> Mono.deferContextual((ctx) -> {
            ServerWebExchange context = ctx.getOrDefault(ServerWebExchange.class, null);
            String userId = context.getRequest().getHeaders().getFirst("X-User-ID");
            log.info("userId: {}", userId);
            SegmentEventTrace segmentEventTrace = new SegmentEventTrace();
            segmentEventTrace.setUserId(Integer.parseInt(userId));
            return Mono.just(segmentEventTrace);
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
}
