package com.pacvue.segment.event.client;

import com.pacvue.segment.event.buffer.Buffer;
import com.segment.analytics.messages.Message;

public interface BufferSegmentEventClient<T, C extends BufferSegmentEventClient<T, C>> extends SegmentEventClient<T> {
    C buffer(Buffer<T> buffer);
}
