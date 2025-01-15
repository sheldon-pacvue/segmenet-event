package com.pacvue.segment.event.buffer;

import java.io.IOException;

@FunctionalInterface
public interface StopAccept {
    void stop() throws IOException;
}
