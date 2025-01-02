package com.pacvue.segment.event.spring.filter;

import com.pacvue.segment.event.holder.ContextHolder;

import javax.servlet.*;
import java.io.IOException;

public class RequestHolderFilter implements Filter {
    private final ContextHolder<ServletRequest> requestHolder;

    public RequestHolderFilter(ContextHolder<ServletRequest> requestHolder) {
        this.requestHolder = requestHolder;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        requestHolder.setContext(servletRequest);
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            requestHolder.setContext(null);
        }
    }

    @Override
    public void destroy() {

    }
}
