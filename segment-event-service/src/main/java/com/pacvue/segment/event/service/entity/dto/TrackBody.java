package com.pacvue.segment.event.service.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class TrackBody {
    private String name;
    private List<Property> properties;

    @Data
    public static class Property {
        private String property;
        private String title;
        private String type;
        private String example;
    }
}
