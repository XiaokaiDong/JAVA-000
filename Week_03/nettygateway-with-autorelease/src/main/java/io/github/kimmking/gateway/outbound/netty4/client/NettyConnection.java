package io.github.kimmking.gateway.outbound.netty4.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import lombok.NoArgsConstructor;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 从NettyConnectionPool中获取一个连接，使用完之后，应该手动释放
 * 对外暴露ClientHandler的读写方法
 */
@NoArgsConstructor
public class NettyConnection {
    private final NettyConnectionPool connectionPool = NettyConnectionPool.getInstance();

    private String host;
    private int port;
    private Channel channel;
    //private AtomicReference<Channel> channelAtomicReference = new AtomicReference<>();

    /**
     * 调用此形式的构造函数，可以预先获取一个连接
     * @param host  主机地址
     * @param port   端口
     */
    public NettyConnection(String host, int port) {
        this.host = host;
        this.port = port;
        try {
            this.channel = connectionPool.getOneChannel(host, port, this);
            //this.channelAtomicReference.set(this.channel);
        } catch (InterruptedException | TimeoutException e) {

            System.out.println("获取到[" + host +":" + port + "]的连接失败！[" + e.getMessage() + "]");
            this.channel = null;
        }
    }


    /**
     *
     * @param request 请求
     * @param host  主机地址
     * @param port 端口
     * @return 当发生异常时，返回空值
     * @throws InterruptedException，无法获取连接时，抛出超时异常
     */
    public ChannelPromise sendMessageByHttp(FullHttpRequest request, String host, int port) throws InterruptedException, TimeoutException {
        ChannelPromise promise = null;
        //如果连接在使用中，为了实现队头堵塞，需要获取新的连接

        if (channel == null || channel.attr(NettyConnectionPool.IN_USE_ATTR_KEY).get() == true) {
            if (channel != null) {
                //将当前连接的属主置空，这样就可以被回收到连接池中
                //System.out.println("将连接拥有者置空...");
                channel.attr(NettyConnectionPool.OWNER_KEY).set(null);
            }
            channel = connectionPool.getOneChannel(host, port, this);
        }

        this.host = host;
        this.port = port;

        ClientHandler handler = null;
        try {
            handler = (ClientHandler) channel.pipeline().get(NettyConnectionPool.CLIENT_HANDLER);
        }catch (Exception e) {
            System.out.println(e.getMessage());
        }

        if (handler == null) {
            System.out.println("null handler detected!");
        }

        promise = handler.sendMessage(request);
        return promise;
    }

    /**
     *
     * @param request 请求 {@link FullHttpRequest}
     * @return 当发生异常时，返回空值
     * @throws InterruptedException，无法获取连接时，抛出超时异常
     */
    public ChannelPromise sendMessageByHttpWithPrefectchCnnct(FullHttpRequest request) throws InterruptedException, TimeoutException {
        //如果连接在使用中，为了实现队头堵塞，需要获取新的连接
        if (channel == null || channel.attr(NettyConnectionPool.IN_USE_ATTR_KEY).get() == true) {
            if (channel != null) {
                //将当前连接的属主置空，这样就可以被回收到连接池中
                channel.attr(NettyConnectionPool.OWNER_KEY).set(null);
            }
            channel = connectionPool.getOneChannel(host, port, this);
        }

        this.host = host;
        this.port = port;

        ClientHandler handler = (ClientHandler)channel.pipeline().get(NettyConnectionPool.CLIENT_HANDLER);
        ChannelPromise promise = handler.sendMessage(request);
        return promise;
    }

    public FullHttpResponse sendMessageByHttpSync(FullHttpRequest request, String host, int port) throws InterruptedException, TimeoutException {
        //如果连接在使用中，为了实现队头堵塞，需要获取新的连接
        if (channel == null || channel.attr(NettyConnectionPool.IN_USE_ATTR_KEY).get() == true) {
            if (channel != null) {
                //将当前连接的属主置空，这样就可以被回收到连接池中
                channel.attr(NettyConnectionPool.OWNER_KEY).set(null);
            }
            channel = connectionPool.getOneChannel(host, port, this);
        }
        this.host = host;
        this.port = port;
        ClientHandler handler = (ClientHandler)channel.pipeline().get(NettyConnectionPool.CLIENT_HANDLER);
        ChannelPromise promise = handler.sendMessage(request);
        promise.await(5, TimeUnit.SECONDS);
        return handler.getResponse();
    }

    /**
     * 手动释放当前连接。可以不调用这个方法，因为从NettyConnectionPool获取的连接在HTTP读取完成完后都是自动释放的
     */
    public void release() {
        if(channel != null) {
            connectionPool.releaseChannel(host, port, channel);
            channel = null;
            host = null;
        }
    }

    /**
     * 手动释放指定连接。可以不调用这个方法，因为从NettyConnectionPool获取的连接在HTTP读取完成完后都是自动释放的
     * @param channel 需要释放的连接
     */
    public void release(Channel channel) {
        if(channel != null) {
            InetSocketAddress address = (InetSocketAddress)channel.remoteAddress();
            connectionPool.releaseChannel(address.getHostName(), address.getPort(), channel);
            channel = null;
            host = null;
        }
    }

    public void close() {
        if(channel != null) {
            channel.close();
        }
    }

    public void makeUsable(Channel channel) {
        channel.attr(NettyConnectionPool.IN_USE_ATTR_KEY).set(false);
    }
}
