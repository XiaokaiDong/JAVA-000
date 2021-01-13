package io.tt.mq.database.mysql.processor;

import io.tt.mq.template.processor.ProducingProcessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 基于MySql的消息生产者处理器
 * 为了实现简单，直接
 */
public class MySqlProducingProcessor implements ProducingProcessor<Connection, String> {
    @Override
    public void accept(Connection connection, String destination) {
        String insertSql = "insert into tb_messages (destination, content, " +
                "processed, create_time" +
                "values (?, ?, ?, ?)";

        try {
            PreparedStatement stmt = connection.prepareStatement(insertSql);
            for (int i = 0; i < 100; i++) {
                stmt.setString(1, destination);
                stmt.setString(2,i + " message.");
                stmt.setInt(3, 0);
                stmt.setLong(4, System.currentTimeMillis());
                stmt.executeUpdate();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
