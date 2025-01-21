package com.pacvue.segment.event.client;

import cn.hutool.core.date.DateUtil;
import com.segment.analytics.messages.Message;
import io.netty.handler.codec.http.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;


import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Builder
public class SegmentEventClientHttp<T extends Message> extends AbstractBufferSegmentEventClient<T, SegmentEventClientHttp<T>> {
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
    private final Consumer<? super HttpHeaders> headerBuilder;
    @Builder.Default
    @NonNull
    private final Function<List<T>, Mono<String>> bodyJsonFactory = Body::generate;

    @Override
    public Mono<Boolean> send(List<T> events) {
        return httpClient
                .headers(headers-> {
                    headers.add(HttpHeaderNames.AUTHORIZATION, secret);
                    headers.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
                    headers.add(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON);
                    if (null != headerBuilder) {
                        headerBuilder.accept(headers);
                    }
                })
                .request(HttpMethod.valueOf(method)).uri(uri)
                .send((req, out) -> out.sendString(bodyJsonFactory.apply(events)))
                .responseSingle((response, body) -> {
                    if (HttpResponseStatus.OK.equals(response.status())) {
                        return Mono.just(true); // 正常处理
                    } else {
                        log.warn("send failed, response: {}", response);
                        return Mono.error(new RuntimeException("HTTP Error: " + response.status())); // 抛出异常
                    }
                })
                .retry(retry);
    }

    @Override
    public void flush() {

    }

    @Data
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Body<T extends Message> {
        public final static String SEND_AT_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

        private List<T> batch;
        private String sendAt;

        public static <T extends Message> Mono<String> generate(List<T> events) {
            Body<T> body = new Body<T>().setBatch(events).setSendAt(DateUtil.format(new Date(), SEND_AT_FORMAT));
            return Mono.just(gson.toJson(body));
        }
    }
}
