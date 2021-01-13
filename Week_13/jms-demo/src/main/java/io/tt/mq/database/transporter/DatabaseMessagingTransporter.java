package io.tt.mq.database.transporter;

import io.tt.mq.template.transporter.MessagingTransporter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 基于数据库实现消息传送
 * 使用JDBC的的Connection表示上下文，即数据库的会话
 * 使用String表示消息的容器——数据库的消息表中消息类别字段
 */
@Slf4j
@Data
abstract public class DatabaseMessagingTransporter implements MessagingTransporter<Connection, String> {
    protected String urlBroker;
    protected DataSource connectionFactory;
    private Connection session;
    //protected Statement session;


    protected DatabaseMessagingTransporter(String urlBroker) {
        this.urlBroker = urlBroker;
        init();
    }

    private void init() {
        if (this.urlBroker == null){
            log.info("The broker has not been designated, init failed!");
        }

        initConnectionFactory();

        try {
            session = connectionFactory.getConnection();
            //session = connection.createStatement();
        } catch (SQLException throwables) {
            throwables.printStackTrace();

        }
    }

    public void close() {

        try {
            session.close();
            //session.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    /**
     * 初始化连接工厂，子类实现。
     */
    abstract public void initConnectionFactory();

}
