package com.pacvue.segment.event.client;

import com.segment.analytics.messages.Message;

public interface SegmentEventClientRegistry {
    SegmentEventClient<Message> getClient(String type);
}
