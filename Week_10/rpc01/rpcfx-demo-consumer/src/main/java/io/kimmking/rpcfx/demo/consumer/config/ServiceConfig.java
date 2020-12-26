package io.kimmking.rpcfx.demo.consumer.config;

import io.kimmking.rpcfx.client.aop.annotation.RpcfxService;
import io.kimmking.rpcfx.demo.api.Order;
import io.kimmking.rpcfx.demo.api.OrderService;
import io.kimmking.rpcfx.demo.api.User;
import io.kimmking.rpcfx.demo.api.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {
    @Bean
    OrderService orderService() {
        return new OrderService() {
            @Override
            @RpcfxService(url = "http://localhost:8080/")
            public Order findOrderById(int id) {
                return null;
            }
        };
    }

    @Bean
    UserService userService() {
        return new UserService() {
            @Override
            @RpcfxService(url = "http://localhost:8080/")
            public User findById(int id) {
                return null;
            }
        };
    }
}
