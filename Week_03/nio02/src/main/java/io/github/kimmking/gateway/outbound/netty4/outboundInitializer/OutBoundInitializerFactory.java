package io.github.kimmking.gateway.outbound.netty4.outboundInitializer;

import io.github.kimmking.gateway.router.RoutingContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

import java.util.HashMap;
import java.util.Map;

public class OutBoundInitializerFactory {
    private static final Map<RoutingContext.DstSchemaType, ChannelInitializer<SocketChannel>> initializers = new HashMap<>();

    static {
        initializers.put(RoutingContext.DstSchemaType.HTTP, new NettyHttpOutboundInitializer());
    }

    public static ChannelInitializer getChannelInitializer(RoutingContext.DstSchemaType type) {
        return initializers.get(type);
    }
}
