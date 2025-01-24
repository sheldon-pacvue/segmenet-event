package com.pacvue.segment.event.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.pacvue.segment.event.client.SegmentEventClientMybatisFlex;
import com.pacvue.segment.event.core.SegmentEventReporter;
import com.pacvue.segment.event.entity.SegmentEventLogMessage;
import com.pacvue.segment.event.gson.GsonConstant;
import com.pacvue.segment.event.service.entity.po.SegmentEventLog;
import com.segment.analytics.messages.Message;
import com.segment.analytics.messages.TrackMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

@SpringBootTest
class SegmentEventServiceApplicationTests implements GsonConstant {
    private static final int THREAD_COUNT = 10;
    private static final int EVENTS_PER_THREAD = 20;
    private final static ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

    @Autowired
    private SegmentEventClientMybatisFlex<SegmentEventLogMessage, SegmentEventLog> client;



    @Test
    void concurrentSendTest() throws InterruptedException, ExecutionException {
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(executorService.submit(() -> {
                List<SegmentEventLogMessage> events = new ArrayList<>();
                for (int j = 0; j < EVENTS_PER_THREAD; j++) {
                    TrackMessage message = TrackMessage.builder("123")
                            .userId(RandomUtil.randomString(10))
                            .anonymousId(RandomUtil.randomString(10))
                            .sentAt(new Date()).build();
                    String msg = gson.toJson(message);
                    SegmentEventLogMessage eventLogMessage = new SegmentEventLogMessage()
                            .eventTime(message.sentAt())
                            .userId(RandomUtil.randomString(10))
                            .type(Message.Type.track.name())
                            .hash(DigestUtil.md5Hex(msg))
                            .message(msg)
                            .reported(false)
                            .operation(SegmentEventReporter.LOG_OPERATION_SEND_TO_INDIRECT)
                            .createdAt(new Date());

                    events.add(eventLogMessage); // 构造事件对象
                }
                return client.send(events).block(); // 使用 block() 让 Mono 转换为同步执行
            }));
        }

        // 关闭线程池，等待所有任务完成
        executorService.shutdown();
        boolean finished = executorService.awaitTermination(1, TimeUnit.MINUTES);

        // 检查所有任务的执行结果
        for (Future<Boolean> future : futures) {
            assert future.get(); // 确保每批数据都成功写入
        }

        assert finished : "Executor did not finish within the timeout!";
    }

}
