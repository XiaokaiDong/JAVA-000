package jdbc.config;

import jdbc.datasource.hsharding.HShardingDataSourceProperties;
import jdbc.datasource.hsharding.HShardingDataSource;
import jdbc.sharding.algorithm.impl.ModHShardingAlgorithm;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class HShardingDataSourceConfig {
    @Bean
    @ConfigurationProperties("h_sharding.datasource")
    HShardingDataSourceProperties shardingSourceProperties() {
        return new HShardingDataSourceProperties();
    }

    @Bean
    HShardingDataSource shardingDataSources(HShardingDataSourceProperties shardingSourceProperties) {
        HShardingDataSource shardingDataSources = new HShardingDataSource();
        shardingDataSources.init(shardingSourceProperties);
        shardingDataSources.setShardingAlgorithm(shardingSourceProperties.getShardingAlgorithm());
        shardingDataSources.setShardingKey(shardingSourceProperties.getShardingKey());
        return shardingDataSources;
    }

    @Bean
    JdbcTemplate shardingJdbcTemplate(HShardingDataSource hShardingDataSource){
        return new JdbcTemplate(hShardingDataSource);
    }

    @Bean
    ModHShardingAlgorithm modHShardingAlgorithm(HShardingDataSourceProperties shardingSourceProperties){
        ModHShardingAlgorithm result = new ModHShardingAlgorithm();
        result.setShardingDataSourceProperties(shardingSourceProperties);
        return result;
    }
}
