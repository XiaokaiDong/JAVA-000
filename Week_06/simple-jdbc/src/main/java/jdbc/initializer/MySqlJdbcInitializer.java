package jdbc.initializer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MySqlJdbcInitializer extends SimpleJdbcInitializer{
    @Override
    protected void worker(Connection connection) throws SQLException {
        String insertSql = "insert into orders (order_id, order_version, create_time, modified_time," +
                "skuid, sku_version, from_id, to_id,final_price, status) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement stmt = connection.prepareStatement(insertSql);

        long timeStamp = 0;
        String orderVersion = "1";
        String tmpStr = null;
        String status = String.valueOf(0);
        for (int i = 0; i < 100_000; i++) {
            tmpStr = String.valueOf(i);

            timeStamp = System.currentTimeMillis();
            stmt.setInt(1, i + 1);
            stmt.setInt(2, 1);
            stmt.setLong(3, timeStamp);
            stmt.setLong(4, timeStamp);
            stmt.setString(5, tmpStr);
            stmt.setString(6, orderVersion);
            stmt.setString(7, tmpStr);
            stmt.setString(8, String.valueOf(i + 1));
            stmt.setLong(9, 10000);
            stmt.setString(10, status);
            stmt.executeUpdate();
        }
    }
}
