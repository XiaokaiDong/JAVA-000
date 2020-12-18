package io.kimmking.rpcfx.client.channel;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class NettyConnectionPool {

    //线程组
    private final EventLoopGroup group = new NioEventLoopGroup();

    //启动类
    private final Bootstrap bootstrap = new Bootstrap();

    //可以向每个目标URL发起的最大连接数（和CHROME类似，限制向每个网站发起的连接数为6个）
    private final static int MAX_CONNECTIONS_PER_URL = 6;

    //存储建立的连接，键的形式为host:port，到特定的host:port可以建立多个连接
    //使用ArrayBlockingQueue而不是使用Set来存储连接，是为了实现HTTP1.1中的队头堵塞
    private Map<String, ArrayBlockingQueue<Channel>> pool = new ConcurrentHashMap<>();

    public static final String CLIENT_HANDLER = "ClientHandler";

    /**
     * 采用单例模式，构造函数私有
     */
    private NettyConnectionPool() {
        init();
    }

    private static class NettyConnectionPoolHolder {
        static final NettyConnectionPool INSTANCE = new NettyConnectionPool();
    }

    public static NettyConnectionPool getInstance() {
        return NettyConnectionPoolHolder.INSTANCE;
    }

    /**
     * 从连接池中获取一个共HTTP1.1使用的连接，当这个连接不存在时，创建；当连接存在时直接使用，
     * 注意HTTP1.1存在队头阻塞的现象，一个TCP连接上，不同的HTTP请求应答声明周期不可以重合
     * @param host 主机地址
     * @param port 端口
     * @return
     */
    public Channel getOneChannel(String host, int port) {
        String key = host + ":" + port;
        ArrayBlockingQueue<Channel> channelArrayBlockingQueue = pool.get(key);

        if (channelArrayBlockingQueue == null) {
            channelArrayBlockingQueue = new ArrayBlockingQueue<>(MAX_CONNECTIONS_PER_URL);
            pool.putIfAbsent(key, channelArrayBlockingQueue);
        }

        Channel channel = null;

        //因为HTTP1.1存在对头堵塞的问题，所以当现有连接没有超过MAX_CONNECTIONS_PER_URL时，都选择新建连接
        synchronized (channelArrayBlockingQueue) {
            if (channelArrayBlockingQueue.size() < MAX_CONNECTIONS_PER_URL)
                createConnection(host, port, channelArrayBlockingQueue);
        }

        //获取一个连接，当连接被使用时，阻塞调用线程，实现了所谓“队头阻塞”
        channel = channelArrayBlockingQueue.poll();

        return channel;
    }

    /**
     * 释放底层TCP连接，共其他HTTP请求使用
     * 可以考虑把String host, int port, Channel channel进一步封装，提高易用性
     * @param host 连接对应的主机地址
     * @param port 端口
     */
    public void releaseChannel(String host, int port, Channel channel) {
        String key = host + ":" + port;
        ArrayBlockingQueue<Channel> channelArrayBlockingQueue = pool.get(key);
        channelArrayBlockingQueue.add(channel);
    }

    /**
     * 创建连接并放入集合
     * @param host IP地址
     * @param port 端口
     * @param channelQueue 存放连接的集合
     * @return
     */
    private void createConnection(String host, int port, Queue<Channel> channelQueue) {
        ChannelFuture channelFuture = null;
        Channel result = null;
        try {
            channelFuture = bootstrap.connect(host,port).sync();
        } catch (InterruptedException e) {
            log.info("Connect to %s : %d failed: [%s]", host, port, e.getLocalizedMessage());
            result = null;
        }

        if (channelFuture != null) {
            channelFuture.channel().closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    //关闭时移除这个连接
                    channelQueue.remove(future.channel());
                }
            });
            result = channelFuture.channel();
            channelQueue.add(result);
        }
    }

    public void init(){
        bootstrap.group(group)
        //长连接
        .option(ChannelOption.SO_KEEPALIVE, true)
        .channel(NioSocketChannel.class)
        .handler(new ChannelInitializer<Channel>() {
            protected void initChannel(Channel channel) throws Exception {

                //包含编码器和解码器
                channel.pipeline().addLast(new HttpClientCodec());

                //聚合
                channel.pipeline().addLast(new HttpObjectAggregator(512 * 1024));

                //解压
                channel.pipeline().addLast(new HttpContentDecompressor());

                channel.pipeline().addLast(CLIENT_HANDLER, new ClientHandler());
            }
        });
    }

    public void shutdown() {
        group.shutdownGracefully();
    }

}
