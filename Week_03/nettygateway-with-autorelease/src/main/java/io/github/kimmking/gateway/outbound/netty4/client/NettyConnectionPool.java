package io.github.kimmking.gateway.outbound.netty4.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

//@Slf4j
public class NettyConnectionPool {

    private static Logger logger = LoggerFactory.getLogger(NettyConnectionPool.class);

    //线程组
    private final EventLoopGroup group = new NioEventLoopGroup(20);

    //启动类
    private final Bootstrap bootstrap = new Bootstrap();

    //可以向每个目标URL发起的最大连接数（和CHROME类似，限制向每个网站发起的连接数为6个）
    private final static int MAX_CONNECTIONS_PER_URL = 120;

    //存储建立的连接，键的形式为host:port，到特定的host:port可以建立多个连接
    //使用ArrayBlockingQueue而不是使用Set来存储连接，是为了实现HTTP1.1中的队头堵塞
    private final Map<String, ArrayBlockingQueue<Channel>> pool = new ConcurrentHashMap<>();
    //某个HOST + PORT的现有连接数量
    private final Map<String, AtomicInteger> numOfConnections = new ConcurrentHashMap<>();
    //管理子连接池的锁
    private final Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    public static final String CLIENT_HANDLER = "ClientHandler";

    //当前连接是否在使用中
    public static final AttributeKey<Boolean> IN_USE_ATTR_KEY = AttributeKey.newInstance("inUse");
    //当前连接的拥有人
    public static final AttributeKey<Object> OWNER_KEY = AttributeKey.newInstance("owner");
    //当前连接是否在连接池中，用于归还连接时的去重
    public static final AttributeKey<Boolean> IN_THE_POOL = AttributeKey.newInstance("inThePool");

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
    public Channel getOneChannel(String host, int port, Object owner) throws InterruptedException, TimeoutException {
        String key = host + ":" + port;

        //System.out.println("使用的键值为: " + key);

        ArrayBlockingQueue<Channel> channelArrayBlockingQueue = pool.get(key);
        AtomicInteger currConnections = numOfConnections.get(key);
        ReentrantLock rtl = lockMap.get(key);


        if (channelArrayBlockingQueue == null) {
            synchronized (pool){

                //再次检查状态，“三位一体”
                channelArrayBlockingQueue = pool.get(key);
                currConnections = numOfConnections.get(key);
                rtl = lockMap.get(key);
                if (channelArrayBlockingQueue == null) {
                    channelArrayBlockingQueue = new ArrayBlockingQueue<>(MAX_CONNECTIONS_PER_URL);
                    pool.putIfAbsent(key, channelArrayBlockingQueue);

                    currConnections = new AtomicInteger(0);
                    numOfConnections.putIfAbsent(key, currConnections);

                    rtl = new ReentrantLock();
                    lockMap.putIfAbsent(key, rtl);

                    //System.out.println("=======为连接 " + key + "创建锁====" + rtl.hashCode());
                }

            }
        }

        //Channel channel = null;

        //尝试获取连接进行复用
        Channel channel = channelArrayBlockingQueue.poll(0, TimeUnit.SECONDS);
        //如果可用连接为空，且没有达到连接数上限，创建连接
        if (channel == null) {
            System.out.println("没有获取到连接，准备新建...");
            rtl.lock();
            try{
                //再次检查连接数量
                if (currConnections.get() < MAX_CONNECTIONS_PER_URL)
                    channel = createConnection(host, port, channelArrayBlockingQueue, currConnections);
            } finally {
                rtl.unlock();
            }
        }

        if (channel != null){
            try {
                //创建的连接可以直接使用，因为上面还没有尚未完成的HTTP请求
                channel.attr(OWNER_KEY).set(owner);
            }catch (Exception e) {
                System.out.println(e.getMessage());
            }
        } else {
            //连接数已经达到上限，只好从队列中获取
            //获取一个连接，当连接被使用时，阻塞调用线程，实现了所谓“队头阻塞”
            channel = channelArrayBlockingQueue.poll(10, TimeUnit.SECONDS);
            if (channel == null) {
                throw new TimeoutException("获取连接超时！");
            }else {
                System.out.println("从连接池中取到连接");
            }
        }

        //不设置channel的使用状态，由使用方决定如何设置

        //取出连接池标志
        channel.attr(IN_THE_POOL).set(false);
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
        if (channelArrayBlockingQueue == null){
            System.out.println("channelArrayBlockingQueue is null and key is: " + key );
        }
        //此时不改变目前已经有的连接数，只是把它还回来。只有连接关闭的时候，才会改变目前的连接数
        if (channel == null)
            System.out.println("检测到空channel!");
        try {
            //设置为未使用状态
            channel.attr(IN_USE_ATTR_KEY).set(false);
            //置属主为空
            channel.attr(OWNER_KEY).set(null);
            //设置已放入连接池标志
            channel.attr(IN_THE_POOL).set(true);
            //放入连接池
            channelArrayBlockingQueue.add(channel);

        }catch (Exception e) {
            System.out.println("发生错误，队列长度超长! " + e.getMessage());
            //e.printStackTrace();
        }
    }

    public void releaseChannel(Channel channel) {
        InetSocketAddress address = (InetSocketAddress)channel.remoteAddress();
        String host = address.getHostString();
        int port = address.getPort();

        String key = host + ":" + port;
        ArrayBlockingQueue<Channel> channelArrayBlockingQueue = pool.get(key);
        //此时不改变目前已经有的连接数，只是把它还回来。只有连接关闭的时候，才会改变目前的连接数
        if (channel == null) {
            System.out.println("检测到空channel!");
            return;
        }
        try {
            //设置为未使用状态
            channel.attr(IN_USE_ATTR_KEY).set(false);
            //设置属主为空
            channel.attr(OWNER_KEY).set(null);
            //设置已放入连接池标志
            channel.attr(IN_THE_POOL).set(true);
            //放入连接池
            channelArrayBlockingQueue.add(channel);
        }catch (Exception e) {
            System.out.println("发生错误，队列长度超长!");
            //e.printStackTrace();
            System.out.println("现有连接数： " + numOfConnections.get(key).get());
            System.out.println("队列长度：" + channelArrayBlockingQueue.size());
            for (Channel chl:channelArrayBlockingQueue) {
                System.out.println(chl.id());
            }
            System.exit(-1);
        }
    }

    /**
     * 创建连接并放入集合
     * @param host IP地址
     * @param port 端口
     * @param channelQueue 存放连接的集合
     * @param currConnections 当前连接（使用IP + PORT区分）的数量
     * @return
     */
    private Channel createConnection(String host, int port, Queue<Channel> channelQueue, AtomicInteger currConnections) {
        ChannelFuture channelFuture = null;
        Channel result = null;

        //System.out.println("线程 [" + Thread.currentThread().getId() + "]进入createConnection");

        try {
            channelFuture = bootstrap.connect(host, port).sync();
            //logger.info("Connect to {} : {} succeed: [{}]", host, port);
            //System.out.println("Connect to [" + host + ":" + port + "]succeed!");
            //System.out.println("创建了第" + currConnections.get() + "个连接");
            //增加连接计数
            currConnections.getAndIncrement();
        } catch (InterruptedException e) {
            //logger.info("Connect to {} : {} failed: [{}]", host, port, e.getLocalizedMessage());
            System.out.println("Connect to [" + host + ":" + port + "]failed! [" + e.getLocalizedMessage() + "]");
            result = null;
        }

        if (channelFuture != null) {
            channelFuture.channel().closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    //关闭时移除这个连接，只有当这个连接在队列中时才移除它
                    if (channelQueue.contains(future.channel()))
                        channelQueue.remove(future.channel());
                    currConnections.decrementAndGet();
                    //logger.info("连接{}被关闭", future.channel().remoteAddress());
                    System.out.println("连接被关闭[" + future.channel().remoteAddress() +"]");
                }
            });
            result = channelFuture.channel();

            //设置channel为未使用状态
            result.attr(IN_USE_ATTR_KEY).set(false);
            //没有属主
            result.attr(OWNER_KEY).set(null);
            //还没有放入连接池
            result.attr(IN_THE_POOL).set(false);
            //下面被注释掉，新建的连接直接使用，不影响队头堵塞；连接被使用完后自动回收
            //channelQueue.add(result);
            System.out.println(host + port +"的总连接数为: " + currConnections.get());
            System.out.println("目前连接池中的连接数为[应该为0]： " + channelQueue.size());
            System.out.println("channel with id [" + result.id() + "] is created!");
        }
        System.out.println("线程 [" + Thread.currentThread().getId() + "]离开createConnection");
        return result;
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
