package com.pacvue.segment.event.entity;

import com.pacvue.segment.event.entity.annotation.SegmentEventType;
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
@SegmentEventType("trace")
public class SegmentEventTrace extends SegmentEvent {
    private String anonymousId;
}