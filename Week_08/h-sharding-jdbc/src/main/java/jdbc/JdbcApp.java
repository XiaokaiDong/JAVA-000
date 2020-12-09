package jdbc;

import jdbc.services.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;

import java.sql.SQLException;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class})
public class JdbcApp implements CommandLineRunner {

    private QueryService queryService;

    @Autowired
    public JdbcApp(QueryService queryService) {
        this.queryService = queryService;
    }

    public static void main(String[] args) throws Exception {
        //doWithoutConnectionPool();

        SpringApplication.run(JdbcApp.class, args);

    }


    @Override
    public void run(String... args) throws SQLException {
       queryService.selectSth();
    }


}
