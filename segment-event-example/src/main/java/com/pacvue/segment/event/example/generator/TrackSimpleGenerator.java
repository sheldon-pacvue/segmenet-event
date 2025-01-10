package com.pacvue.segment.event.example.generator;

import com.pacvue.segment.event.generator.SimpleSegmentEventGenerator;
import com.segment.analytics.messages.MessageBuilder;
import com.segment.analytics.messages.TrackMessage;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@Component
public class TrackSimpleGenerator extends SimpleSegmentEventGenerator<TrackMessage, TrackMessage.Builder> {
    @Override
    public MessageBuilder<TrackMessage, TrackMessage.Builder> getObject() {
        return TrackMessage.builder("Item Purchased")
                .userId("f4ca124298")
                .anonymousId("123123")
                .properties(Map.of(
                "revenue", 39.95,
                "shipping", "2-day"
                ))
                .sentAt(new Date());
    }
}
