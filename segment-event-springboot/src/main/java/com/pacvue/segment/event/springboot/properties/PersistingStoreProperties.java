package com.pacvue.segment.event.springboot.properties;

import com.pacvue.segment.event.gson.Gson;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

import static com.pacvue.segment.event.springboot.properties.PersistingStoreProperties.PROPERTIES_PREFIX;

@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class PersistingStoreProperties<T> implements Gson {
    public final static String PROPERTIES_PREFIX = "segment.event.store.persisting";

    private Class<T> clazz;
    private Map<String, Object> properties;

    public T getConfig() {
        return gson.fromJson(gson.toJson(properties), clazz);
    }
}
