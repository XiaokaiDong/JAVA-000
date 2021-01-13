package io.tt.mq.jms.activemq.processor;


import io.tt.mq.template.processor.ConsumingProcessor;

import javax.jms.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ActiveMQConsumingProcessor implements ConsumingProcessor<Session, Destination> {

    @Override
    public void accept(Session session, Destination destination) {
        try {
            // 创建消费者
            MessageConsumer consumer = session.createConsumer( destination );
            final AtomicInteger count = new AtomicInteger(0);
            MessageListener listener = new MessageListener() {
                public void onMessage(Message message) {
                    try {
                        // 打印所有的消息内容
                        // Thread.sleep();
                        System.out.println(count.incrementAndGet() + " => receive from " + destination.toString() + ": " + message);
                        // message.acknowledge(); // 前面所有未被确认的消息全部都确认。

                    } catch (Exception e) {
                        e.printStackTrace(); // 不要吞任何这里的异常，
                    }
                }
            };
            // 绑定消息监听器
            consumer.setMessageListener(listener);

            //consumer.receive()
        }catch (JMSException e) {

        }
    }
}
