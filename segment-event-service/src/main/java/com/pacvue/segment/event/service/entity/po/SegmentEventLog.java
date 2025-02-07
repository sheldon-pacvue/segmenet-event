package com.pacvue.segment.event.service.entity.po;

import cn.hutool.crypto.digest.DigestUtil;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.pacvue.segment.event.entity.MessageLog;
import com.pacvue.segment.event.entity.SegmentEventLogMessage;
import com.pacvue.segment.event.gson.GsonConstant;
import com.segment.analytics.messages.Message;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

@Data
@Table(value = "SegmentEventLog", schema = "default", dataSource = "clickhouse", camelToUnderline = false)
@Accessors(chain = true, fluent = true)
public class SegmentEventLog implements MessageLog<SegmentEventLog>, GsonConstant {
    @Column(typeHandler = DateToIntTypeHandler.class)
    private Date eventTime;
    private Date eventDate;
    private String hash;
    private String userId;
    private String type;
    @Column(typeHandler = MessageTypeHandler.class)
    private Message message;
    private boolean result;
    private short operation;
    @Column(typeHandler = DateToIntTypeHandler.class)
    private Date createdAt;

    @NotNull
    @Override
    public Type type() {
        return message.type();
    }

    @NotNull
    @Override
    public String messageId() {
        return message.messageId();
    }

    @Nullable
    @Override
    public Date sentAt() {
        return message.sentAt();
    }

    @NotNull
    @Override
    public Date timestamp() {
        return message.timestamp();
    }

    @Nullable
    @Override
    public Map<String, ?> context() {
        return message.context();
    }

    @Nullable
    @Override
    public String anonymousId() {
        return message.anonymousId();
    }

    @Nullable
    @Override
    public Map<String, Object> integrations() {
        return message.integrations();
    }

    @Override
    public SegmentEventLog covert(SegmentEventLogMessage message) {
        return covert(message.message())
                .result(message.result())
                .operation(message.operation());
    }

    @Override
    public SegmentEventLog covert(Message message) {
        return this
                .eventTime(message.timestamp())
                .eventDate(message.timestamp())
                .hash(DigestUtil.md5Hex(gson.toJson(message)))
                .userId(message.userId())
                .type(message.type().name())
                .message(message)
                .createdAt(new Date());
    }

    public static class DateToIntTypeHandler extends BaseTypeHandler<Date> {

        @Override
        public void setNonNullParameter(PreparedStatement ps, int i, Date parameter, JdbcType jdbcType) throws SQLException {
            // 将 Date 转换为 Unix 时间戳
            ps.setInt(i, (int) (parameter.getTime() / 1000));  // 转为秒级时间戳
        }

        @Override
        public Date getNullableResult(ResultSet rs, String columnName) throws SQLException {
            int timestamp = rs.getInt(columnName);
            return new Date(timestamp * 1000L);  // 转换回 Date 类型
        }

        @Override
        public Date getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            int timestamp = rs.getInt(columnIndex);
            return new Date(timestamp * 1000L);  // 转换回 Date 类型
        }

        @Override
        public Date getNullableResult(java.sql.CallableStatement cs, int columnIndex) throws SQLException {
            int timestamp = cs.getInt(columnIndex);
            return new Date(timestamp * 1000L);  // 转换回 Date 类型
        }
    }

    public static class MessageTypeHandler extends BaseTypeHandler<Message> implements GsonConstant {
        @Override
        public void setNonNullParameter(PreparedStatement ps, int i, Message parameter, JdbcType jdbcType) throws SQLException {
            ps.setString(i, gson.toJson(parameter));
        }

        @Override
        public Message getNullableResult(ResultSet rs, String columnName) throws SQLException {
            return gson.fromJson(rs.getString(columnName), Message.class);
        }

        @Override
        public Message getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            return gson.fromJson(rs.getString(columnIndex), Message.class);
        }

        @Override
        public Message getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
            return gson.fromJson(cs.getString(columnIndex), Message.class);
        }
    }
}
