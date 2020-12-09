package jdbc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@SpringBootApplication
public class JdbcApp implements CommandLineRunner {

    @Resource
    private DataSource dataSource;

    public static void main(String[] args) throws Exception {
        //doWithoutConnectionPool();

        SpringApplication.run(JdbcApp.class, args);

    }


    @Override
    public void run(String... args) throws SQLException {
        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
            connection = dataSource.getConnection();
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
