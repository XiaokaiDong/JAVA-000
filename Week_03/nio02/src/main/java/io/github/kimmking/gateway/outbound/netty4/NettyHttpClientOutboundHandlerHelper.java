package io.github.kimmking.gateway.outbound.netty4;

import io.github.kimmking.gateway.outbound.netty4.common.ChannelProperties;
import io.github.kimmking.gateway.outbound.netty4.outboundInitializer.OutBoundInitializerFactory;
import io.github.kimmking.gateway.outbound.netty4.registry.impl.HttpBootstrapRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class NettyHttpClientOutboundHandlerHelper {

    private ChannelHandlerContext ctx;

    private ChannelFuture connectFuture;

    //是否已经连接
    private AtomicBoolean connected = new AtomicBoolean(false);
    //是否正在连接
    private AtomicBoolean connecting = new AtomicBoolean(false);

    public NettyHttpClientOutboundHandlerHelper(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public void handle(final FullHttpRequest httpRequest) {
        //是否需要连接
        if (!connected.get() && !connecting.get()) {

            connecting.compareAndSet(false,true);

            Channel srcChannel = ctx.channel();
            String srcUrl = srcChannel.attr(ChannelProperties.SRC_URL.getAttributeKey()).get();

            Bootstrap bootstrap = HttpBootstrapRegistry.getRegistry().getConnectionManager(srcUrl);

            bootstrap.handler(OutBoundInitializerFactory
                    .getChannelInitializer(HttpBootstrapRegistry.getRegistry().getSchemaType(srcUrl)));

            bootstrap.group(ctx.channel().eventLoop());
            connectFuture = bootstrap.connect();
            connectFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(connectFuture.isSuccess()){
                        connecting.compareAndSet(true, false);
                        connected.compareAndSet(false, true);
                        connectFuture.channel().flush();
                    }else {
                        Throwable cause = future.cause();
                        log.info("connect to [ apMerch ]failed!" + cause);
                        //关闭入站方向的通道
                        ctx.channel().close();
                        //移除相应的客户端Bootstrap
                        HttpBootstrapRegistry.getRegistry().remove(srcUrl);
                    }
                }
            });
        }

        if(connectFuture.isDone()) {
            connectFuture.channel().writeAndFlush(httpRequest);
        }else {
            connectFuture.channel().write(httpRequest);
        }
    }
}
