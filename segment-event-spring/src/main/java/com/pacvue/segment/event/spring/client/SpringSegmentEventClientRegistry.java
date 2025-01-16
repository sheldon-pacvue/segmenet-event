package com.pacvue.segment.event.spring.client;

import com.pacvue.segment.event.client.SegmentEventClient;
import com.pacvue.segment.event.client.SegmentEventClientRegistry;
import com.segment.analytics.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SpringSegmentEventClientRegistry implements SegmentEventClientRegistry {
    private final Map<String, SegmentEventClient<Message>> clientMap = new HashMap<>();

    @Autowired
    public SpringSegmentEventClientRegistry(List<? extends SegmentEventClient<Message>> clients) {
        for (SegmentEventClient<Message> client : clients) {
            clientMap.put(client.getType(), client);
        }
    }

    @Override
    public SegmentEventClient<Message> getClient(String type) {
        return clientMap.get(type);
    }
}
