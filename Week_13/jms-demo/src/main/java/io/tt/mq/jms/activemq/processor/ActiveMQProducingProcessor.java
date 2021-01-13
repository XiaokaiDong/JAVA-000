package io.tt.mq.jms.activemq.processor;

import io.tt.mq.template.processor.ProducingProcessor;

import javax.jms.*;

public class ActiveMQProducingProcessor implements ProducingProcessor<Session, Destination> {

    @Override
    public void accept(Session session, Destination destination) {
        // 生产100个消息
        try {
            MessageProducer producer = session.createProducer(destination);
            int index = 0;
            while (index++ < 100) {
                TextMessage message = session.createTextMessage(index + " message.");
                producer.send(message);
            }
        }catch (JMSException e) {
            e.printStackTrace();
        }
    }


}
