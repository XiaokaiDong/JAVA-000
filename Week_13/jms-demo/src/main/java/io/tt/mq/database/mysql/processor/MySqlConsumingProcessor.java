package io.tt.mq.database.mysql.processor;

import io.tt.mq.template.processor.ConsumingProcessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySqlConsumingProcessor implements ConsumingProcessor<Connection, String> {
    private final static String processorID = System.getenv("HOSTNAME") + "_" + Thread.currentThread().getId();

    @Override
    public void accept(Connection connection, String destination) {
        //标记数据，相当于打一个快照
        String markSql = "update tb_messages set processed = 2, processor_id = ? where destination = ?" +
                "and processed = 0 limit 20";

        //读取打了标记的快照
        String querySql = "select content from tb_messages where destination = ?" +
                " and processed = 2 and processor_id = ? order by create_time";

        //对处理过的数据进行打标
        String makeProcessed = "update tb_messages set processed = 1, processor_id = ? where destination = ?" +
                "and processed = 2 limit 20";

        String message = null;
        try {
            PreparedStatement markStmt = connection.prepareStatement(markSql);
            markStmt.setString(1, processorID);
            markStmt.setString(2, destination);
            markStmt.executeUpdate();

            PreparedStatement stmt = connection.prepareStatement(querySql);
            stmt.setString(1, destination);
            stmt.setString(2, processorID);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {
                message = rs.getString(1);
                System.out.println(" => receive from " + destination + ": " + message);
            }

            PreparedStatement processedStmt = connection.prepareStatement(makeProcessed);
            processedStmt.setString(1, processorID);
            processedStmt.setString(2, destination);
            processedStmt.executeUpdate();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
