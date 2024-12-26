package com.pacvue.segment.event.core;

import com.fasterxml.uuid.Generators;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class SegmentEvent implements Serializable {
    @Serial
    private final static long serialVersionUID = 452880117401445296L;

    private final String messageId = Generators.timeBasedEpochRandomGenerator().generate().toString();
    private final long timestamp = System.currentTimeMillis();
    private int userId;
    private String event;
    private final Map<String, Object> properties = new HashMap<>();
    private final Map<String, Object> context = new HashMap<>();
    private final Map<String, Object> integrations = new HashMap<>();
}
