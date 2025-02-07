package com.pacvue.segment.event.entity;

import com.pacvue.segment.event.gson.GsonConstant;
import com.segment.analytics.messages.Message;

public interface MessageLog<T extends MessageLog<T>> extends Message, GsonConstant {
    T result(boolean result);

    T operation(short operation);

    T covert(Message message);

    T covert(SegmentEventLogMessage message);
}
