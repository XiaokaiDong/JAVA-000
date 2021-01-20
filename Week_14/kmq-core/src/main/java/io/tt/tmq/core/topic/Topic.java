package io.tt.tmq.core.topic;

import io.tt.tmq.core.brokers.Broker;
import io.tt.tmq.core.partition.TQQueue;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 主题，由若干个分区或者队列组成。
 * Topic只是多个分区逻辑上的容器，做一些簿记工作。主题的元信息，应该只保留在ZooKeeper或类似的地方
 */
public class Topic {
    //主题的分区数
    private int numQueue;

    //主题的副本数
    private int replicaFactor;

    //本主题的分区或者队列，包括主副副本
    private final CopyOnWriteArrayList<String> partitions = new CopyOnWriteArrayList<>();


    //本主题的家目录，其所有的分区/队列TQQueue是这个目录下的子目录，子目录下是日志段文件TQLog
    private String basePath;

    //本主题所在的Broker列表
    private final CopyOnWriteArrayList<Broker> hostBrokers = new CopyOnWriteArrayList<>();

    public Topic(int numQueue, int replicaFactor, String basePath) {
        this.numQueue = numQueue;
        this.replicaFactor = replicaFactor;
        this.basePath = basePath;

    }
}
