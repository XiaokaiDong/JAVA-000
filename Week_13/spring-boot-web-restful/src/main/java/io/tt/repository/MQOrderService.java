package io.tt.repository;

import com.alibaba.fastjson.JSON;
import io.tt.model.Order;
import kafka.common.KafkaException;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service("orderService")
public class MQOrderService implements OrderService{

    private Properties producerProperties;
    private KafkaProducer<String, String> producer;

    private Properties consumerProperties;
    private KafkaConsumer<String, String> consumer;

    //主题名称
    private String topic;   //"order.usd2cny";

    //生产者事务ID
    private static final String txID = "USD2CNY";

    //提交位移的频率
    private static final int commitOffsetAfterNumRecs = 100;

    //消费者线程消费的消息数量
    private static final ThreadLocal<Integer> NUM_RECORDS_CONSUMMER = new ThreadLocal<>();

    static {
        NUM_RECORDS_CONSUMMER.set(0);
    }

    //更精细的管理消费者提交的位移
    private Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();

    @Autowired
    JdbcTemplate jdbcTemplate;

    public MQOrderService(String topic) {
        this.topic = topic;

        //创建生产者属性
        producerProperties = new Properties();

        //生产者一般属性
        producerProperties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProperties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProperties.put("bootstrap.servers", "localhost:9092");

        //创建消费者属性
        consumerProperties = new Properties();

        //消费者一般属性
        consumerProperties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        consumerProperties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        consumerProperties.put("bootstrap.servers", "localhost:9092");

        //1、保证消息不丢 begin=============================

        //1.1、生产者端相关设置
        producerProperties.put("acks", "all");
        //这一条会自动启动上一条，其实只打开这一条就够了
        producerProperties.put("enable.idempotence", "true");
        //设置 retries 为一个较大的值
        producerProperties.put("retries", "3");

        /**
         * Broker端参数
         * unclean.leader.election.enable = false
         * replication.factor >= 3
         * min.insync.replicas > 1
         * replication.factor = min.insync.replicas + 1
         */

        //1.2、消费着端相关设置
        //关闭自动提交
        consumerProperties.put("enable.auto.commit", "false");
        //所有的消费者的组ID都和本订单服务所属的主题相同
        consumerProperties.put("group.id", topic);

        //保证消息不丢 end=============================

        //2、消息不重复相关设置 begin=============================
        //2.1、生产者端相关设置

        //打开幂等性开关
        producerProperties.put("enable.idempotence", "true");
        //设置事务的ID
        producerProperties.put("transactional. id", txID);

        //2.2、消费者端相关设置
        consumerProperties.put("read_committed", "true");

        //消息不重复相关设置 end=============================

        //保证消息有序
        //所有相同的键都会分配到同一个分区中——分区中的消息是保证顺序的
        producerProperties.put("max.in.flight.requests.per.connection", "1");

        //创建生产者
        producer = new KafkaProducer<String, String>(producerProperties);

    }

    /**
     * 生成消费者
     * 由于KAFKA的消费着是单线程模型，为了提高消费的并发型，提供了这个办法
     * @return
     */
    public KafkaConsumer<String, String> createConsumer(){
        return new KafkaConsumer<String, String>(consumerProperties);
    }

    public KafkaProducer<String, String> createProducer() {
        return new KafkaProducer<String, String>(consumerProperties);
    }


    /**
     * 按顺序提交订单。使用了事务型producer来保证消息不重复
     * @param order
     */
    @Override
    public void sequencing(Order order) {
        producer.initTransactions();
        try{
            producer.beginTransaction();
            ProducerRecord record =
                    new ProducerRecord(topic, order.getId().toString(), JSON.toJSONString(order));
            producer.commitTransaction();
        }catch (KafkaException e){
            producer.abortTransaction();
        }

    }

    /**
     * 消费消息，保证消息顺序消费、可重放、消息仅处理一次
     * 消息仅处理一次的保证方法：
     *   创建消费者的时候，需要设置一定的属性，参见构造函数中的初始化过程
     * 有序性的保证方法：
     *   生产者生产消息时，会以订单ID作为消息的键，这样就会按照键值发送的一个固定的分区中。订单ID需要具有唯一性的特点；
     *   由于在分布式或者很高的并发下，ID生成很难再额外满足单调递增的特定，所以此处只要求具备唯一性。
     * 可重放的保证：
     *   手动管理位移。提交位移时采用同步提交和异步提交相结合的方式。将位移存放到数据库中，保证再次重启后，从其中
     *   拿到上次的唯一消息。
     * @param order
     */
    @Override
    public void match(Order order) {
        consumer.subscribe(Collections.singletonList(topic), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                for (TopicPartition partition: partitions) {
                    saveOffsetToDb(partition);
                }
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                partitions.forEach(topicPartition -> consumer.seek(topicPartition, getOffsetFromDB(topicPartition)));
            }
        });

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord record : records) {
                    processOrder(record);
                    offsets.put(new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1));
                    if (NUM_RECORDS_CONSUMMER.get() % commitOffsetAfterNumRecs == 0) {
                        // 使用异步提交规避阻塞
                        consumer.commitAsync(offsets, null);
                        saveRecordAndOffsetInDB(record, record.offset() + 1);
                    }

                }

            }
        }catch (Exception e) {

        }finally {
            try{
                consumer.commitSync();
            }finally {
                consumer.close();
            }
        }
    }

    private long getOffsetFromDB(TopicPartition topicPartition) {
        String sql = "select offset from tb_mq_offset where topic = ? and partition = ?";
        return jdbcTemplate.queryForObject(sql, long.class,
                topicPartition.topic(), topicPartition.partition());
    }

    /**
     * 将当前消费者消费的最新的唯一记录入数据库
     * @param partition
     */
    private void saveOffsetToDb(TopicPartition partition) {
        String sql = "update tb_mq_offset set offset = ?, updated_time = ? where topic = ? and partition = ?";
        Map<TopicPartition, Long> map = new HashMap<>();
        long currTime = System.currentTimeMillis();
        map.put(partition, currTime);
        jdbcTemplate.update(sql, consumer.offsetsForTimes(map, Duration.ofMillis(100)), currTime,
                partition.topic(), partition.partition());
    }

    /**
     * 将消费的位移保存到数据库
     * @param record  当前提交位移的最新记录
     * @param offset  当前记录的位移
     */
    private void saveRecordAndOffsetInDB(ConsumerRecord record, long offset) {
        String sql = "update tb_mq_offset set offset = ?, updated_time = ? where topic = ? and partition = ?";
        jdbcTemplate.update(sql, offset, System.currentTimeMillis(), this.topic, record.partition());
    }

    private void processOrder(ConsumerRecord<String, String> record) {
        ConsumerRecord<String, String> r = (ConsumerRecord) record;
            Order order = JSON.parseObject(r.value(), Order.class);

            //判断是否已经处理过此订单
            String sql = "select count(*) from orders where order_id = ?";
            long processed = jdbcTemplate.queryForObject(sql, long.class, order.getId());
            if (processed >= 1){
                return;
            }

            System.out.println(" order = " + order);

    }
}
