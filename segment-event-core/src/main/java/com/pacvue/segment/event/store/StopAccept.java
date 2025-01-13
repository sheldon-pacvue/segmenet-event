package com.pacvue.segment.event.store;

import java.io.IOException;

@FunctionalInterface
public interface StopAccept {
    void stop() throws IOException;
}
