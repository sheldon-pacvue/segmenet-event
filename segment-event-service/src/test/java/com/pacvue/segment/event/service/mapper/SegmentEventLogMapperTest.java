package com.pacvue.segment.event.service.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.pacvue.segment.event.service.entity.po.SegmentEventLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.pacvue.segment.event.service.mapper.table.SegmentEventLogTableDef.SEGMENT_EVENT_LOG;

@SpringBootTest
class SegmentEventLogMapperTest {
    @Autowired
    private SegmentEventLogMapper segmentEventLogMapper;

    @Test
    public void test() {
        QueryWrapper queryWrapper = QueryWrapper.create().select().where(SEGMENT_EVENT_LOG.HASH.eq("3e5c3632fc05602b54754c55be1ccba3"));
        SegmentEventLog log = segmentEventLogMapper.selectOneByQuery(queryWrapper);
        System.out.println(log);
    }
}