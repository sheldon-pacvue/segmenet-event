package com.pacvue.segment.event.service.entity.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.segment.analytics.messages.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true, fluent = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY) // 自动检测字段
public final class Batch {
  private List<Message> batch;
  private Date sentAt;
  private Map<String, ?> context;
  private int sequence;
  private String writeKey;
}
