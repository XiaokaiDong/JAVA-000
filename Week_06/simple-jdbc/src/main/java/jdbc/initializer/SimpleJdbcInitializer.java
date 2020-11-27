package jdbc.initializer;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Data
public abstract class SimpleJdbcInitializer {
    private DataSource dataSource;


    public void doWithConnectionPool() throws Exception{
        System.out.println("--------------WithConnectionPool---------------");

        Connection connection = dataSource.getConnection();
        worker(connection);

        ((HikariDataSource) dataSource).close();

        System.out.println("--------------END WithConnectionPool---------------");
    }

    protected abstract void worker(Connection connection) throws SQLException;


}
