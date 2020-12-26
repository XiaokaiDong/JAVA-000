package io.kimmking.rpcfx.demo;

import io.kimmking.rpcfx.demo.registry.LocalRegistryConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConsumerConfiguraton {
    @Bean
    LocalRegistryConfiguration localRegistryConfiguration(){
        return new LocalRegistryConfiguration();
    }
}
