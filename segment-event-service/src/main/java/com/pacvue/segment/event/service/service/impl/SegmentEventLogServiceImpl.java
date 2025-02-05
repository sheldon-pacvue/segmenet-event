package com.pacvue.segment.event.service.service.impl;

import com.mybatis.flex.reactor.spring.ReactorServiceImpl;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.gson.GsonConstant;
import com.pacvue.segment.event.service.entity.dto.ResendSegmentEventDTO;
import com.pacvue.segment.event.service.entity.po.SegmentEventLog;
import com.pacvue.segment.event.service.mapper.SegmentEventLogMapper;
import com.pacvue.segment.event.service.service.SegmentEventLogService;
import com.segment.analytics.messages.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cursor.Cursor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import static com.pacvue.segment.event.service.entity.po.table.SegmentEventLogTableDef.SEGMENT_EVENT_LOG;

@Slf4j
@Service
public class SegmentEventLogServiceImpl extends ReactorServiceImpl<SegmentEventLogMapper, SegmentEventLog> implements SegmentEventLogService, GsonConstant {
    @Autowired
    private SegmentIO segmentIO;

    @Override
    public Flux<SegmentEventLog> getEventLogs(ResendSegmentEventDTO body) {
        return Flux.<SegmentEventLog>create(sink -> Db.tx(() -> {
                    QueryWrapper queryWrapper = QueryWrapper.create().select()
                            .where(SEGMENT_EVENT_LOG.RESULT.eq(body.getResult()))
                            .and(SEGMENT_EVENT_LOG.CREATED_AT.gt(body.getFrom().getTime() / 1000))
                            .and(SEGMENT_EVENT_LOG.CREATED_AT.lt(body.getTo().getTime() / 1000))
                            .and(SEGMENT_EVENT_LOG.TYPE.eq(body.getType()))
                            .and(SEGMENT_EVENT_LOG.OPERATION.eq(body.getOperation()));

                    Cursor<SegmentEventLog> logs = mapper.selectCursorByQuery(queryWrapper);
                    Iterator<SegmentEventLog> iterator = logs.iterator();

                    sink.onDispose(() -> {
                        try {
                            logs.close();
                        } catch (Exception e) {
                            sink.error(e);
                        }
                    });

                    try {
                        while (iterator.hasNext()) {
                            SegmentEventLog log = iterator.next();
                            sink.next(log);
                        }
                        sink.complete();
                    } catch (Exception ex) {
                        sink.error(ex);
                        return false;
                    }
                    return true;
                }), FluxSink.OverflowStrategy.BUFFER)
                .subscribeOn(Schedulers.boundedElastic()); // 让数据库查询在专门的线程池中执行
    }

    @Override
    public void resendEventLogs(ResendSegmentEventDTO body) {
        AtomicInteger i = new AtomicInteger(0);
        getEventLogs(body)
                .publishOn(Schedulers.boundedElastic())
                .subscribe(data -> {
                    log.info("count: {}", i.addAndGet(1));
                    segmentIO.deliverReact(gson.fromJson(data.getMessage(), Message.class)).subscribe();
                });
    }

    @Override
    public Mono<Boolean> saveEventLog(SegmentEventLog log) {
        return Mono.fromCallable(() -> mapper.insert(log) > 0)
                .subscribeOn(Schedulers.boundedElastic()); // 切换到阻塞适配线程池
    }
}
