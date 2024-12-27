package com.pacvue.segment.event.client;

import cn.hutool.core.date.DateUtil;
import com.pacvue.segment.event.core.SegmentEvent;
import io.netty.handler.codec.http.HttpMethod;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;


import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Builder
@RequiredArgsConstructor
public class SegmentEventClientHttp implements SegmentEventClient {
    @NonNull
    private final HttpClient httpClient;
    @NonNull
    private final String method;
    @NonNull
    private final String uri;
    @NonNull
    private final Integer retry;
    @NonNull
    private final String secret;
    @Builder.Default
    @NonNull
    private final Function<List<SegmentEvent>, Body> bodyFactory = Body::generate;

    @Override
    public Mono<Boolean> send(List<SegmentEvent> events) {
        return httpClient
                .headers(headers-> {
                    headers.add("Authorization", secret);
                    headers.add("Content-Type", "application/json");
                    headers.add("Accept", "application/json");
                })
                .request(HttpMethod.valueOf(method)).uri(uri)
                .send((req, out) -> out.sendObject(bodyFactory.apply(events)))
                .response().flatMap(response -> {
                    if (response.status().code() != 200) {
                        return Mono.just(false);
                    }
                    return Mono.just(true);
                }).retry(retry);
    }

    @Data
    @Builder
    public static class Body {
        public final static String SEND_AT_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

        private List<SegmentEvent> batch;
        private String sendAt;

        public static Body generate(List<SegmentEvent> events) {
            return Body.builder().batch(events).sendAt(DateUtil.format(new Date(), SEND_AT_FORMAT)).build();
        }
    }


}
