package com.pacvue.segment.event.springboot.properties;

import java.util.Map;

import com.pacvue.segment.event.gson.Gson;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public abstract class AbstractObjectProperties<T> implements Gson {
  private Class<T> clazz;
  private Map<String, Object> properties;

  public T getConfig() {
    return gson.fromJson(gson.toJson(properties), clazz);
  }
}
