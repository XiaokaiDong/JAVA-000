package io.github.kimmking.gateway.router;

import io.netty.bootstrap.Bootstrap;
import lombok.Data;

import java.net.URL;

@Data
public class RoutingContext {

    //目标连接管理器
    private Bootstrap connectionManager;

    //目标URL
    private URL dstUrl;

    public enum DstSchemaType {
        TCP,
        HTTP,
        HTTPS;
    }

    private DstSchemaType dstSchemaType;
}
