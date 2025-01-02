package com.pacvue.segment.event.spring.filter;

import javax.servlet.*;
import java.io.IOException;

public class ServletRequestHolderFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            ServletRequestHolder.set(servletRequest);
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            ServletRequestHolder.remove();
        }
    }

    @Override
    public void destroy() {

    }
}
