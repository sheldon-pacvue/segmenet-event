package com.pacvue.segment.event.service.entity.po;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.pacvue.segment.event.entity.SegmentEventLogMessage;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

@Data
@Table(value = "SegmentEventLog", schema = "default", dataSource = "clickhouse", camelToUnderline = false)
@Accessors(chain = true)
public class SegmentEventLog {
    private Date eventDate;
    private String hash;
    private String userId;
    private String type;
    private String message;
    private boolean result;
    private short operation;
    // 使用自定义序列化和反序列化
    @Column(typeHandler = DateToIntTypeHandler.class)
    private Date createdAt;
    // 使用自定义序列化和反序列化
    @Column(typeHandler = DateToIntTypeHandler.class)
    private Date eventTime;

    public static SegmentEventLog fromMessage(SegmentEventLogMessage message) {
        return new SegmentEventLog()
                .setEventDate(message.eventTime())
                .setEventTime(message.eventTime())
                .setHash(message.hash())
                .setUserId(message.userId())
                .setType(message.type())
                .setMessage(message.message())
                .setResult(message.reported())
                .setOperation(message.operation())
                .setCreatedAt(message.createdAt());
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

}
