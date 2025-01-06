package com.pacvue.segment.event.entity;

import com.pacvue.segment.event.entity.annotation.SegmentEventType;
import com.pacvue.segment.event.generator.SegmentEvent;
import com.pacvue.segment.event.generator.SegmentEventGenerator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SegmentEventClassRegistry {
    private final static Map<String, Class<? extends SegmentEvent>> classRegistry = new HashMap<>();

    static {
        try {
            SegmentEventClassRegistry.register("com.pacvue.segment.event.entity");
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<? extends SegmentEvent> getSegmentEventClass(String type) {
        return classRegistry.get(type);
    }

    public static void register(String... packageNames) throws IOException, ClassNotFoundException {
        for (String packageName : packageNames) {
            classRegistry.putAll(SegmentEventScanner.scanSegmentEventTypes(packageName));
        }
    }

    public static class SegmentEventScanner {
        // 扫描指定包中的所有类，并返回符合条件的类
        public static Map<String, Class<? extends SegmentEvent>> scanSegmentEventTypes(String packageName) throws ClassNotFoundException, IOException {
            Map<String, Class<? extends SegmentEvent>> eventMap = new HashMap<>();

            // 获取指定包的所有类
            File directory = new File(SegmentEventScanner.class.getClassLoader().getResource(packageName.replace('.', '/')).getFile());
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".class"));

            if (files != null) {
                for (File file : files) {
                    String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6); // 去掉.class后缀
                    Class<?> clazz = Class.forName(className);

                    // 检查类是否使用了 SegmentEventType 注解并且是 SegmentEvent 的子类
                    if (clazz.isAnnotationPresent(SegmentEventType.class) && SegmentEvent.class.isAssignableFrom(clazz)) {
                        SegmentEventType segmentEventType = clazz.getAnnotation(SegmentEventType.class);
                        eventMap.put(segmentEventType.value(), (Class<? extends SegmentEvent>) clazz);
                    }
                }
            }
            return eventMap;
        }
    }
}