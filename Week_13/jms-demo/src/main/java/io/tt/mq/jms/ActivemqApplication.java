package io.tt.mq.jms;

import io.tt.mq.jms.activemq.ActivateMQJmsMessagingTransporter;
import io.tt.mq.jms.activemq.processor.ActiveMQConsumingProcessor;
import io.tt.mq.jms.activemq.processor.ActiveMQProducingProcessor;
import io.tt.mq.template.transporter.MessagingTransporter;
import org.apache.activemq.command.ActiveMQQueue;

import javax.jms.Destination;

public class ActivemqApplication {
    public static void main(String[] args) {
        // 定义Destination
        //Destination destination = new ActiveMQTopic("test.topic");
        Destination destination = new ActiveMQQueue("test.queue");

        testDestination(destination);
    }

    private static void testDestination(Destination destination) {
        MessagingTransporter mq = new ActivateMQJmsMessagingTransporter("tcp://127.0.0.1:61616");
        mq.initConnectionFactory();

        //消费消息
        mq.consumeMessage(new ActiveMQConsumingProcessor(), mq.getSession(), destination);

        //生产消息
        mq.produceMessage(new ActiveMQProducingProcessor(), mq.getSession(), destination);

        try {
            Thread.sleep(2000);
            mq.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
