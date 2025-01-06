package com.pacvue.segment.event.entity.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SegmentEventScans {
    String[] packageName() default {};
}
