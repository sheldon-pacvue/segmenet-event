package com.pacvue.segment.event.service.service.impl;

import com.mybatis.flex.reactor.spring.ReactorServiceImpl;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.gson.GsonConstant;
import com.pacvue.segment.event.service.entity.dto.ResendSegmentEventDTO;
import com.pacvue.segment.event.service.entity.po.SegmentEventLog;
import com.pacvue.segment.event.service.mapper.SegmentEventLogMapper;
import com.pacvue.segment.event.service.service.SegmentEventLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.sql.SQLException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static com.pacvue.segment.event.service.entity.po.table.SegmentEventLogTableDef.SEGMENT_EVENT_LOG;

@Slf4j
@Service
public class SegmentEventLogServiceImpl extends ReactorServiceImpl<SegmentEventLogMapper, SegmentEventLog> implements SegmentEventLogService, GsonConstant {
    @Autowired
    private SegmentIO segmentIO;

    @Override
    public Flux<SegmentEventLog> getEventLogs(ResendSegmentEventDTO body) {
        QueryWrapper queryWrapper = QueryWrapper.create().select()
                .where(SEGMENT_EVENT_LOG.RESULT.eq(body.getResult()))
                .and(SEGMENT_EVENT_LOG.CREATED_AT.gt(body.getFrom().getTime() / 1000))
                .and(SEGMENT_EVENT_LOG.CREATED_AT.lt(body.getTo().getTime() / 1000))
                .and(SEGMENT_EVENT_LOG.TYPE.eq(body.getType()))
                .and(SEGMENT_EVENT_LOG.OPERATION.eq(body.getOperation()));
        return list(queryWrapper)
                .subscribeOn(Schedulers.boundedElastic()); // 让查询在独立线程池执行
    }


    @Override
    public void resendEventLogs(ResendSegmentEventDTO body) {
        AtomicInteger i = new AtomicInteger(0);
        getEventLogs(body)
                .publishOn(Schedulers.boundedElastic())
                .subscribe(data -> {
                    segmentIO.deliverReact(data.message()).subscribe();
                });
    }

    @Override
    public Mono<Boolean> saveEventLog(SegmentEventLog log) {
        return Mono.fromCallable(() -> mapper.insert(log) > 0)
                .subscribeOn(Schedulers.boundedElastic()); // 切换到阻塞适配线程池
    }
}
