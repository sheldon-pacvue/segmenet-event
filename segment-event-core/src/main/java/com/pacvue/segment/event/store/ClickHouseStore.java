package com.pacvue.segment.event.store;

import com.pacvue.segment.event.entity.SegmentEventDataBase;
import com.pacvue.segment.event.generator.SegmentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class ClickHouseStore<T extends SegmentEvent> implements Store<SegmentEventDataBase<T>> {
    /**
     * TODO 写入数据库
     */
    @Override
    public Mono<Boolean> publish(SegmentEventDataBase<T> event) {

        return Mono.just(Boolean.TRUE);
    }

    /**
     * TODO 从数据库中查询，并且上报，注意分布式问题，使用zookeeper，redis等其他分布式系统进行选举master进行查询上报
     *
     * 未发送成功的数据拿出来再发一次
     * $query = 'SELECT MAX(eventDate), hash, MAX(type) as type, MAX(userId) as userId, SUM(result) as result, '
     *             . ' MAX(message) as message, MIN(eventTime) as eventTime FROM ' . SegmentEventsLog::tableName()
     *             . ' WHERE eventDate >= \'' . $dateFrom . '\' '
     *             . ' AND operation = ' . SegmentIo::LOG_OPERATION_SEND_TO_SEGMENT
     *             . ' GROUP BY hash HAVING result = 0 ORDER BY eventTime';
     *
     * 没搞懂这个有什么用？
     * 将在sqs队列中的数据拿出来重发,发完后删除队列的中数据
     * $query = 'SELECT * FROM ' . SegmentEventsLog::tableName()
     *             . ' WHERE eventDate = \'' . $date . '\' AND type = \'track\''
     *             . ' AND operation = ' . SegmentIo::LOG_OPERATION_SEND_TO_SQS;
     */
    @Override
    public void subscribe(Consumer<List<SegmentEventDataBase<T>>> consumer, int bundleCount) {

    }

    @Override
    public void shutdown() {

    }
}
