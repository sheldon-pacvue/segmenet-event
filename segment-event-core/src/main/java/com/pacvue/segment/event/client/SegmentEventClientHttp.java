package com.pacvue.segment.event.client;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.pacvue.segment.event.core.SegmentEvent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import lombok.*;
import lombok.experimental.Accessors;
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
    private final Function<List<SegmentEvent>, Mono<String>> bodyJsonFactory = Body::generate;

    @Override
    public Mono<Boolean> send(List<SegmentEvent> events) {
        return httpClient
                .headers(headers-> {
                    headers.add(HttpHeaderNames.AUTHORIZATION, secret);
                    headers.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
                    headers.add(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON);
                })
                .request(HttpMethod.valueOf(method)).uri(uri)
                .send((req, out) -> out.sendString(bodyJsonFactory.apply(events)))
                .response().flatMap(response -> {
                    if (response.status().code() != 200) {
                        return Mono.just(false);
                    }
                    return Mono.just(true);
                }).retry(retry);
    }

    @Data
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Body {
        public final static String SEND_AT_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

        private List<SegmentEvent> batch;
        private String sendAt;

        public static Mono<String> generate(List<SegmentEvent> events) {
            Body body = new Body().setBatch(events).setSendAt(DateUtil.format(new Date(), SEND_AT_FORMAT));
            return Mono.just(JSONUtil.toJsonStr(body));
        }
    }


}
