package com.pacvue.segementeventexample.filter;

import com.pacvue.segment.event.holder.TtlContextHolder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
public class RequestHolderFilter implements WebFilter {
    @Autowired
    private TtlContextHolder<String> contextHolder;

    @Override
    public @NonNull Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        contextHolder.setContext(exchange.getRequest().getHeaders().getFirst("X-Request-ID"));
        log.info("filter: {}", contextHolder.getContext());
        return chain.filter(exchange);
    }
}
