package com.pacvue.segment.event.spring.filter;

import com.alibaba.ttl.TransmittableThreadLocal;

import javax.servlet.ServletRequest;

public class ServletRequestHolder {
    private final static TransmittableThreadLocal<ServletRequest> requestHolder = new TransmittableThreadLocal<>();

    public static void set(ServletRequest request) {
        requestHolder.set(request);
    }

    public static ServletRequest get() {
        return requestHolder.get();
    }

    public static void remove() {
        requestHolder.remove();
    }
}
