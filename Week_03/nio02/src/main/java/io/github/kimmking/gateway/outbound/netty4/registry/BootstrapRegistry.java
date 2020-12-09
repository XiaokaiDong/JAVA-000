package io.github.kimmking.gateway.outbound.netty4.registry;

import io.netty.bootstrap.Bootstrap;


/**
 * 根据id返回一个Bootstrap用于连接被代理的服务
 */
public interface BootstrapRegistry {

    Bootstrap getConnectionManager(String id);

    void remove(String id);
}
