package jdbc.config;

import jdbc.initializer.MySqlJdbcInitializer;
import jdbc.initializer.OracleJdbcInitializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {
    @Bean
    @ConfigurationProperties("oracle.datasource")
    public DataSourceProperties oracleDatasourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource oracleDatasource(@Qualifier("oracleDatasourceProperties") DataSourceProperties dataSourceProperties){
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean
    @ConfigurationProperties("mysql.datasource")
    public DataSourceProperties mysqlDatasourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource mysqlDatasource(@Qualifier("mysqlDatasourceProperties") DataSourceProperties dataSourceProperties){
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean
    public MySqlJdbcInitializer  mySqlJdbcInitializer(DataSource mysqlDatasource) {
        MySqlJdbcInitializer initializer = new MySqlJdbcInitializer();
        initializer.setDataSource(mysqlDatasource);
        return initializer;
    }

    @Bean
    public OracleJdbcInitializer  oracleJdbcInitializer(DataSource oracleDatasource) {
        OracleJdbcInitializer initializer = new OracleJdbcInitializer();
        initializer.setDataSource(oracleDatasource);
        return initializer;
    }
}
