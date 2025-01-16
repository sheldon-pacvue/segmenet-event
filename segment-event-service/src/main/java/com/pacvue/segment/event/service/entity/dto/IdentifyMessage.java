package com.pacvue.segment.event.service.entity.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.segment.analytics.messages.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true, fluent = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY) // 自动检测字段
public final class IdentifyMessage implements Message {
  private Type type;
  private String messageId;
  private Date sentAt;
  private Date timestamp;
  private Map<String, ?> context;
  private String anonymousId;
  private String userId;
  private Map<String, Object> integrations;
  private Map<String, ?> traits;
}
