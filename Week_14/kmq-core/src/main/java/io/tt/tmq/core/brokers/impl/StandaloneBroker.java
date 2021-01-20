package io.tt.tmq.core.brokers.impl;

import io.tt.tmq.core.brokers.Broker;
import io.tt.tmq.core.partition.TQQueue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 独立的数据节点
 */
public class StandaloneBroker implements Broker {

    //本节点上的主题数
    private final AtomicInteger numOfTopics = new AtomicInteger(0);

    //本节点上的分区数，包含主副本和追随者副本。按照主题进行分类保存
    private Map<String, CopyOnWriteArraySet<TQQueue>> topicAndTQQueues = new ConcurrentHashMap<>();

    @Override
    public void start() {

    }

    @Override
    public void serve() {

    }

    @Override
    public void createQueue(String queueID, String topic) {
        CopyOnWriteArraySet<TQQueue> tqQueues = topicAndTQQueues.get(queueID);

        if (tqQueues == null) {
            tqQueues = new CopyOnWriteArraySet<TQQueue>();
            topicAndTQQueues.putIfAbsent(topic, tqQueues);
        }

        TQQueue tqQueue = new TQQueue(queueID, topic);
        tqQueues.add(tqQueue);
    }
}
