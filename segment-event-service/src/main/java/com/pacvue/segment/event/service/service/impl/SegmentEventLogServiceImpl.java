package com.pacvue.segment.event.service.service.impl;

import com.mybatis.flex.reactor.spring.ReactorServiceImpl;
import com.pacvue.segment.event.core.SegmentIO;
import com.pacvue.segment.event.gson.GsonConstant;
import com.pacvue.segment.event.service.entity.dto.ResendSegmentEventDTO;
import com.pacvue.segment.event.service.entity.dto.SegmentEventLogCursor;
import com.pacvue.segment.event.service.entity.po.SegmentEventLog;
import com.pacvue.segment.event.service.mapper.SegmentEventLogMapper;
import com.pacvue.segment.event.service.service.SegmentEventLogService;
import com.segment.analytics.messages.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SegmentEventLogServiceImpl extends ReactorServiceImpl<SegmentEventLogMapper, SegmentEventLog> implements SegmentEventLogService, GsonConstant {
    @Autowired
    private SegmentIO segmentIO;
    @Override
    public Flux<Message> getEventLogs(ResendSegmentEventDTO body) {
        return Flux.<Message>create(emitter -> {
            SegmentEventLogCursor cursor = null;
            List<SegmentEventLog> list = new ArrayList<>();
            for (;;) {
                // 获取下游请求的数据量
                long requested = emitter.requestedFromDownstream();

                // 如果 requested == 0，说明消费端繁忙，等待一段时间
                if (requested == 0) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(100); // 100ms 休眠，避免 CPU 过高
                    } catch (InterruptedException e) {
                        log.error("thread sleep failed", e);
                        Thread.currentThread().interrupt();
                        emitter.complete();
                        return;
                    }
                    continue;
                }

                // 消费端要求更多数据时候，进行数据消费，如果数据消费完成，并且没有更多数据，可停止循环
                while (requested > 0) {
                    if (!list.isEmpty()) {
                        emitter.next(list.remove(0).message());
                        requested--;
                    } else {
                        int pageSize = 1000;
                        list = mapper.selectSegmentEventLogByCursor(body.getFrom(), body.getTo(), body.getType(), body.getOperation(), cursor, body.getFocus(), pageSize);
                        if (list.isEmpty()) {
                            emitter.complete();
                            return;
                        } else {
                            SegmentEventLog lastOne = list.get(list.size() - 1);
                            cursor = new SegmentEventLogCursor(lastOne.hash());
                        }
                    }
                }
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public void resendEventLogs(ResendSegmentEventDTO body) {
        segmentIO.deliverReact(getEventLogs(body));
    }

    @Override
    public Mono<Boolean> saveEventLog(SegmentEventLog log) {
        return Mono.fromCallable(() -> mapper.insert(log) > 0)
                .subscribeOn(Schedulers.boundedElastic()); // 切换到阻塞适配线程池
    }
}
