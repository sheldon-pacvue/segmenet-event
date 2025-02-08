package com.pacvue.segment.event.service.mapper;


import com.mybatisflex.core.BaseMapper;
import com.pacvue.segment.event.service.entity.dto.SegmentEventLogCursor;
import com.pacvue.segment.event.service.entity.po.SegmentEventLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface SegmentEventLogMapper extends BaseMapper<SegmentEventLog> {
    List<SegmentEventLog> selectSegmentEventLogByCursor(@Param("from") Date from, @Param("to") Date to, @Param("type") String type,
                                                        @Param("operation") Integer operation, @Param("cursor") SegmentEventLogCursor cursor,
                                                        @Param("focus") boolean focus, @Param("pageSize") Integer pageSize);

}
