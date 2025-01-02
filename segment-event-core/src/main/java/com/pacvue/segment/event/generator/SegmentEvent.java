package com.pacvue.segment.event.generator;

import com.fasterxml.uuid.Generators;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public abstract class SegmentEvent implements Serializable {
    @Serial
    private final static long serialVersionUID = 452880117401445296L;

    private final String messageId = Generators.timeBasedEpochRandomGenerator().generate().toString();
    private final long timestamp = System.currentTimeMillis();
    private String event;
    private final Map<String, Object> properties = new HashMap<>();
    private final Map<String, Object> context = new HashMap<>();
    private final Map<String, Object> integrations = new HashMap<>();

    public abstract String getType();
}
