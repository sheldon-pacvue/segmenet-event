package com.pacvue.segment.event.service.entry.sqs;

import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.messaging.converter.MessageConversionException;

@Slf4j
@Component
public class SqsErrorHandler implements ErrorHandler<Object> {

    @Override
    public void handle(@NotNull Message<Object> message, @NotNull Throwable t) {
        // 当类型是某些类型时候，比如数据类型转换错误，则丢弃消息
        if (hasCauseUsingApache(t, MessageConversionException.class)) {
            log.error("Error processing SQS message, skipping: {}", message.getPayload(), t);
            return;
        }
        log.warn("Error processing SQS message, retry: {}", message.getPayload(), t);
        // 这里抛出异常，消息会进行重试
        throw new UnsupportedOperationException("Single message error handling not implemented");
    }

    @SafeVarargs
    public static boolean hasCauseUsingApache(Throwable throwable, Class<? extends Throwable>... targetTypes) {
        for (Class<? extends Throwable> targetType : targetTypes) {
            if (ExceptionUtils.indexOfType(throwable, targetType) != -1) {
                return true;
            }
        }
        return false;
    }
}