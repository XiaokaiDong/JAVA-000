package jdbc.services;

import annotations.ReadOnly;
import annotations.RoutingStrategy;
import jdbc.datasource.readwrite.DynamicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class QueryService {
    @Autowired
    private DynamicDataSource dynamicDataSource;

    @ReadOnly(readonly = true)
    @RoutingStrategy(name = "RR")
    public void selectSth() throws SQLException {
        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
            connection = dynamicDataSource.getConnection();
            String querySql = "select * from t1";
            pstmt = connection.prepareStatement(querySql);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                System.out.println(id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            pstmt.close();
            connection.close();
        }
    }
}
