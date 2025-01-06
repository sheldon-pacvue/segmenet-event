package com.pacvue.segment.event.entity;

import com.pacvue.segment.event.generator.SegmentEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class SegmentEventPage extends SegmentEvent {
    private String anonymousId;

    @Override
    public String getType() {
        return "page";
    }
}