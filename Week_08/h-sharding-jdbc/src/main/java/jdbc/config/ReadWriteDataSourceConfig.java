package jdbc.config;

import jdbc.datasource.readwrite.DynamicDataSource;
import jdbc.datasource.readwrite.MultiDataSourceProperties;
import jdbc.datasource.readwrite.MultiDataSources;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ReadWriteDataSourceConfig {
    @Bean
    @ConfigurationProperties("readwrite.datasource")
    MultiDataSourceProperties readWriteDataSourceProperties() {
        return new MultiDataSourceProperties();
    }

    @Bean
    MultiDataSources readWriteDataSources(MultiDataSourceProperties readWriteDataSourceProperties) {
        MultiDataSources readWriteDataSources = new MultiDataSources();
        readWriteDataSources.init(readWriteDataSourceProperties);
        return readWriteDataSources;
    }

    @Bean
    @ConfigurationProperties("readonly.datasource")
    MultiDataSourceProperties readonlyDataSourceProperties() {
        return new MultiDataSourceProperties();
    }

    @Bean
    MultiDataSources readonlyDataSources(MultiDataSourceProperties readonlyDataSourceProperties) {
        MultiDataSources readWriteDataSources = new MultiDataSources();
        readWriteDataSources.init(readonlyDataSourceProperties);
        return readWriteDataSources;
    }

    @Bean
    @Primary
    public DynamicDataSource dynamicDataSource(MultiDataSources readWriteDataSources, MultiDataSources readonlyDataSources) {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(Boolean.valueOf(false), readWriteDataSources);
        targetDataSources.put(Boolean.valueOf(true), readonlyDataSources);
        return new DynamicDataSource(readWriteDataSources, targetDataSources);
    }
}
