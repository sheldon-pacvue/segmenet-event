package com.pacvue.segment.event.spring.client;

import com.pacvue.segment.event.client.SegmentEventClient;
import com.pacvue.segment.event.client.SegmentEventClientRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SpringSegmentEventClientRegistry implements SegmentEventClientRegistry {
    private final Map<Class<? extends SegmentEventClient>, SegmentEventClient> clientMap = new HashMap<>();

    @Autowired
    public SpringSegmentEventClientRegistry(List<? extends SegmentEventClient> clients) {
        for (SegmentEventClient client : clients) {
            clientMap.put(client.getClass(), client);
        }
    }

    @Override
    public <T extends SegmentEventClient> T getClient(Class<T> tClass) {
        SegmentEventClient segmentEventClient = clientMap.get(tClass);
        if (!tClass.isInstance(segmentEventClient)) {
            throw new IllegalArgumentException("Segment event client is not registered: " + tClass);
        }
        return tClass.cast(segmentEventClient);
    }
}
