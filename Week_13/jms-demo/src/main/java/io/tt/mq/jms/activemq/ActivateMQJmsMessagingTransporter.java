package io.tt.mq.jms.activemq;

import io.tt.mq.jms.transporter.JmsMessagingTransporter;
import org.apache.activemq.ActiveMQConnectionFactory;

public class ActivateMQJmsMessagingTransporter extends JmsMessagingTransporter {
    public ActivateMQJmsMessagingTransporter(String uriBroker) {
        super(uriBroker);
    }

    @Override
    public void initConnectionFactory() {
        super.connectionFactory = new ActiveMQConnectionFactory(super.uriBroker);
    }


}
