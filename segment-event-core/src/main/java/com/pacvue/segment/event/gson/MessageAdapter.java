package com.pacvue.segment.event.gson;

import com.google.gson.*;
import com.segment.analytics.messages.*;

import java.lang.reflect.Type;

public class MessageAdapter implements JsonSerializer<Message>, JsonDeserializer<Message> {
  @Override
  public JsonElement serialize(Message src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject jsonObject = context.serialize(src).getAsJsonObject();
    // 检查是否已有 "type" 字段，若没有则手动添加
    if (!jsonObject.has("type")) {
      jsonObject.addProperty("type", src.type().name());
    }
    return jsonObject;
  }

  @Override
  public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();
    String type = jsonObject.get("type").getAsString();

    return switch (Message.Type.valueOf(type)) {
      case identify -> context.deserialize(json, IdentifyMessage.class);
      case group -> context.deserialize(json, GroupMessage.class);
      case track -> context.deserialize(json, TrackMessage.class);
      case screen -> context.deserialize(json, ScreenMessage.class);
      case page -> context.deserialize(json, PageMessage.class);
      case alias -> context.deserialize(json, AliasMessage.class);
    };
  }
}
