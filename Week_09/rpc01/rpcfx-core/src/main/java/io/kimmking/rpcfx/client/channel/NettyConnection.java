package io.kimmking.rpcfx.client.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.concurrent.TimeUnit;

/**
 * 对ClientHandler的简单包装
 */
public class NettyConnection {
    private NettyConnectionPool connectionPool = NettyConnectionPool.getInstance();

    private String host;
    private int port;
    private Channel channel;

    public ChannelPromise sendMessageByHttp(FullHttpRequest request, String host, int port) throws InterruptedException {
        //每次都要重新获取连接，已实现队头阻塞
        this.channel = connectionPool.getOneChannel(host, port);
        this.host = host;
        this.port = port;
        ClientHandler handler = (ClientHandler)channel.pipeline().get(NettyConnectionPool.CLIENT_HANDLER);
        return handler.sendMessage(request);
    }

    public FullHttpResponse sendMessageByHttpSync(FullHttpRequest request, String host, int port) throws InterruptedException {
        //每次都要重新获取连接，已实现队头阻塞
        this.channel = connectionPool.getOneChannel(host, port);
        this.host = host;
        this.port = port;
        ClientHandler handler = (ClientHandler)channel.pipeline().get(NettyConnectionPool.CLIENT_HANDLER);
        ChannelPromise promise = handler.sendMessage(request);
        promise.await(30, TimeUnit.SECONDS);
        return handler.getResponse();
    }

    public void release() {
        if(channel != null) {
            connectionPool.releaseChannel(host, port, channel);
            channel = null;
            host = null;
        }
    }

    public void close() {
        if(channel != null) {
            channel.close();
        }
    }
}
