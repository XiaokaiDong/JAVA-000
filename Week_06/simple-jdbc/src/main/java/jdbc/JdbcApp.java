package jdbc;

import jdbc.initializer.MySqlJdbcInitializer;
import jdbc.initializer.OracleJdbcInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class})
public class JdbcApp implements CommandLineRunner {


    @Autowired
    private MySqlJdbcInitializer mysqlInitializer;

    @Autowired
    private OracleJdbcInitializer oracleInitializer;

    public static void main(String[] args) throws Exception {
        //doWithoutConnectionPool();

        SpringApplication.run(JdbcApp.class, args);

    }




    @Override
    public void run(String... args) throws Exception {
        long startTime = System.currentTimeMillis();
        oracleInitializer.doWithConnectionPool();
        startTime = System.currentTimeMillis() - startTime;
        System.out.println("In oracle time used is " + startTime + "ms");

        startTime = System.currentTimeMillis();
        mysqlInitializer.doWithConnectionPool();
        startTime = System.currentTimeMillis() - startTime;
        System.out.println("In mysql time used is " + startTime + "ms");
    }
}
