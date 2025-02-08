package com.pacvue.segment.event.helper;


import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Proxy;
import java.util.concurrent.Semaphore;

public class MethodConcurrentLimitHelper {
    public static <T> T wrap(T target, Class<T> interfaceType, String methodName, int limit) {
        final Semaphore semaphore = new Semaphore(limit); // 限制最大并发数
        return interfaceType.cast(Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                new Class<?>[]{interfaceType}, // 代理单个接口
                (proxy, method, args) -> {
                    if (!StringUtils.equals(method.getName(), methodName)) {
                        return method.invoke(proxy, args);
                    }
                    try {
                        semaphore.acquire(); // 限流
                        Object result = method.invoke(target, args);
                        if (result instanceof Mono<?> mono) {
                            return mono.doFinally(signalType -> semaphore.release());
                        } else if (result instanceof Flux<?> flux) {
                            return flux.doFinally(signalType -> semaphore.release());
                        }
                        semaphore.release();
                        return result;
                    } catch (Exception ex){
                        semaphore.release();
                        throw ex;
                    }
                }
        ));
    }
}
