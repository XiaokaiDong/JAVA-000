package io.tt.tmq.core.brokers;

/**
 * tmq的服务端
 */
public interface Broker {
    void start();
    void serve();
    void createQueue(String queueID, String topic);
}
