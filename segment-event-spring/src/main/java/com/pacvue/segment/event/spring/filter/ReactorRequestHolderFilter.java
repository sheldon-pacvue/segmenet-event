package com.pacvue.segment.event.spring.filter;

import com.pacvue.segment.event.holder.ContextHolder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.servlet.*;
import java.io.IOException;

public class ReactorRequestHolderFilter implements WebFilter {
    private final ContextHolder<ServerHttpRequest> requestHolder;

    public ReactorRequestHolderFilter(ContextHolder<ServerHttpRequest> requestHolder) {
        this.requestHolder = requestHolder;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.defer(() -> {
            requestHolder.setContext(exchange.getRequest());
            return chain.filter(exchange);
        }).doFinally(s -> requestHolder.setContext(null));
    }
}
