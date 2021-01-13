package io.tt.repository;

import io.tt.model.Order;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service("orderService")
public class MQOrderService implements OrderService{

    private Properties properties;
    private KafkaProducer<String, String> producer;
    private final String topic = "order.usd2cny";

    public MQOrderService() {
        properties = new Properties();
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("bootstrap.servers", "localhost:9092");

        //保证消息不丢 begin
        properties.put("acks", "all");
        properties.put("retries", "3");

        /**
         * Broker端参数
         * unclean.leader.election.enable = false
         * replication.factor >= 3
         * min.insync.replicas > 1
         * replication.factor = min.insync.replicas + 1
         */

        //保证消息不丢 end

        producer = new KafkaProducer<String, String>(properties);
    }

    @Override
    public void sequencing(Order order) {

    }

    @Override
    public void match(Order order) {

    }
}
