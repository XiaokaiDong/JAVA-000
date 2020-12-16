package io.kimmking.rpcfx.demo.api;

import io.kimmking.rpcfx.client.aop.annotation.RpcfxService;

public interface OrderService {
    //@RpcfxService(url = "http://localhost:8080/")
    Order findOrderById(int id);

}
