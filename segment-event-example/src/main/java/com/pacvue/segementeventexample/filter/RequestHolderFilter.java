package com.pacvue.segementeventexample.filter;

import com.pacvue.segment.event.holder.TtlContextHolder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
public class RequestHolderFilter implements WebFilter {
    @Autowired
    private TtlContextHolder<Integer> contextHolder;

    @Override
    public @NonNull Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
        contextHolder.setContext(Optional.ofNullable(userId).map(Integer::parseInt).orElse(-1));
        log.info("filter: {}", contextHolder.getContext());
        return chain.filter(exchange);
    }
}
