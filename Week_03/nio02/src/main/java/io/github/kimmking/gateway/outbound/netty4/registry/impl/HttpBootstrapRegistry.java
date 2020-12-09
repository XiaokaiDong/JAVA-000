package io.github.kimmking.gateway.outbound.netty4.registry.impl;


import io.github.kimmking.gateway.outbound.netty4.registry.BootstrapRegistry;
import io.github.kimmking.gateway.router.HttpEndpointRouter;
import io.github.kimmking.gateway.router.RoutingContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class HttpBootstrapRegistry implements BootstrapRegistry, HttpEndpointRouter {
    private Map<String, String> routingTable = new ConcurrentHashMap<>();

    private Map<String, RoutingContext> contextMap = new ConcurrentHashMap<>();

    protected HttpBootstrapRegistry() {}

    public void init(){
        routingTable.put("/Ap1/", "127.0.0.1:8808/Ap1/");
    }

    @Override
    public String route(List<String> endpoints) {
        return routingTable.get(endpoints.get(0));
    }

    //用静态内部类实现一个工厂
    private static class HttpBootstrapRegistryFactory {
        private static final HttpBootstrapRegistry registry = new HttpBootstrapRegistry();
    }

    public static HttpBootstrapRegistry getRegistry() {
        return HttpBootstrapRegistryFactory.registry;
    }

    @Override
    public Bootstrap getConnectionManager(String id) {
        ArrayList<String> list = new ArrayList<String>();

        list.add(id);
        String dstUrl = HttpBootstrapRegistry.getRegistry().route(list);
        Bootstrap bootstrap = contextMap.get(dstUrl).getConnectionManager();

        URL url = null;
        if (bootstrap == null){
            RoutingContext routingContext = new RoutingContext();
            try {
                url = new URL(dstUrl);
                routingContext.setDstUrl(url);

            } catch (MalformedURLException e) {
                log.info("路由表目标URL格式错误: [" + dstUrl + "]");
            }
            bootstrap = new Bootstrap();
            bootstrap.remoteAddress(url.getHost(), url.getPort());
            routingContext.setConnectionManager(bootstrap);
            routingContext.setDstSchemaType(RoutingContext.DstSchemaType.HTTP);
            contextMap.putIfAbsent(id, routingContext);
        }
        return bootstrap;
    }

    @Override
    public void remove(String id) {
        contextMap.remove(id);
    }

    public RoutingContext.DstSchemaType getSchemaType(String id) {
        return contextMap.get(id).getDstSchemaType();
    }
}
