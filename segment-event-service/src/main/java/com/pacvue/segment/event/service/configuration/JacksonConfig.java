package com.pacvue.segment.event.service.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.pacvue.segment.event.service.entity.dto.message.*;
import com.segment.analytics.messages.Message;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Message.class, new MessageDeserializer());
        objectMapper.registerModule(module);
        return objectMapper;
    }

    public static class MessageDeserializer extends JsonDeserializer<Message> {
        @Override
        public Message deserialize(JsonParser p, DeserializationContext dc) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            String type = node.get("type").asText();

            return switch (Message.Type.valueOf(type)) {
                case identify -> p.getCodec().treeToValue(node, IdentifyMessageDTO.class);
                case group -> p.getCodec().treeToValue(node, GroupMessageDTO.class);
                case track -> p.getCodec().treeToValue(node, TrackMessageDTO.class);
                case screen -> p.getCodec().treeToValue(node, ScreenMessageDTO.class);
                case page -> p.getCodec().treeToValue(node, PageMessageDTO.class);
                case alias -> p.getCodec().treeToValue(node, AliasMessageDTO.class);
            };
        }
    }
}
