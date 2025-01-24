package com.pacvue.segment.event.service.service.impl;

import com.mybatis.flex.reactor.spring.ReactorServiceImpl;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.service.entity.dto.ResendSegmentEventDTO;
import com.pacvue.segment.event.service.entity.po.SegmentEventLog;
import com.pacvue.segment.event.service.mapper.SegmentEventLogMapper;
import com.pacvue.segment.event.service.service.SegmentEventLogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cursor.Cursor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


import static com.pacvue.segment.event.service.entity.po.table.SegmentEventLogTableDef.SEGMENT_EVENT_LOG;

@Slf4j
@Service
public class SegmentEventLogServiceImpl extends ReactorServiceImpl<SegmentEventLogMapper, SegmentEventLog> implements SegmentEventLogService {
    @Autowired
    private SegmentIO segmentIO;

    @Override
    public Flux<SegmentEventLog> getEventLogs(ResendSegmentEventDTO body) {
        // 直接使用 Flux.create 创建流，延迟执行
        return Flux.create((FluxSink<SegmentEventLog> fluxSink)  -> {
            Db.tx(() -> {
                QueryWrapper queryWrapper = QueryWrapper.create().select()
                        .where(SEGMENT_EVENT_LOG.RESULT.eq(body.getResult()))
                        .and(SEGMENT_EVENT_LOG.CREATED_AT.gt(body.getFrom().getTime() / 1000))
                        .and(SEGMENT_EVENT_LOG.CREATED_AT.lt(body.getTo().getTime() / 1000))
                        .and(SEGMENT_EVENT_LOG.TYPE.eq(body.getType()))
                        .and(SEGMENT_EVENT_LOG.OPERATION.eq(body.getOperation()));
                try (Cursor<SegmentEventLog> logs = mapper.selectCursorByQuery(queryWrapper)) {
                    for (SegmentEventLog log : logs) {
                        fluxSink.next(log);  // 逐个发出游标中的数据
                    }
                } catch (Exception e) {
                    fluxSink.error(e); // 发生异常时通知
                    return false;
                } finally {
                    fluxSink.complete();  // 完成流
                }
                return true;
            });
        }).subscribeOn(Schedulers.boundedElastic()); // 在专门的线程池中执行阻塞操作
    }

    @Override
    public void resendEventLogs(ResendSegmentEventDTO body) {
        getEventLogs(body)
                .subscribe(data -> {
                    log.info("resend message: {}", data.getMessage());
                    segmentIO.message(data.getMessage());
                });
    }

    @Override
    public Mono<Boolean> saveEventLog(SegmentEventLog log) {
        return Mono.fromCallable(() -> mapper.insert(log) > 0)
                .subscribeOn(Schedulers.boundedElastic()); // 切换到阻塞适配线程池
    }
}
