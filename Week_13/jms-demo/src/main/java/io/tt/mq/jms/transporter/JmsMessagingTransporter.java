package io.tt.mq.jms.transporter;

import io.tt.mq.template.transporter.MessagingTransporter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.jms.*;

/**
 * 基于JMS实现消息传送
 * 使用JMS的Session表示上下文，即JMS的会话
 * 使用JMS的Destination表示消息的容器，可以是队列\分区，也可以是主题
 */
@Slf4j
@Data
abstract public class JmsMessagingTransporter implements MessagingTransporter<Session, Destination> {
    protected String uriBroker;
    protected ConnectionFactory connectionFactory;
    protected Session session;
    private Connection connection;

    protected JmsMessagingTransporter(String uriBroker) {
        this.uriBroker = uriBroker;
        init();
    }

    private void init(){
        if (this.uriBroker == null){
            log.info("The broker has not been designated, init failed!");
        }

        try {
            initConnectionFactory();
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void close(){

        try {
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    /**
     * 初始化连接工厂，子类实现。
     */
    @Override
    abstract public void initConnectionFactory();


}
