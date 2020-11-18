package io.github.kimmking.gateway.outbound.netty4;

import io.github.kimmking.gateway.inbound.HttpInboundHandler;
import io.github.kimmking.gateway.outbound.netty4.dispatcher.OperationResultFuture;
import io.github.kimmking.gateway.router.HttpEndpointRouter;
import io.github.kimmking.gateway.router.impl.TrivialHttpEndpointRouter;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class NettyHttpClientOutboundHandler  extends SimpleChannelInboundHandler {

    private  String host;
    private int port;
    //private String backendUrl;

    private ChannelFuture channelFuture;

    private ChannelHandlerContext srcChannelHandlerContext;

    //如果是Spring可以直接注入一个
    private HttpEndpointRouter httpEndpointRouter = new TrivialHttpEndpointRouter();

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private final Condition connectedCondition = writeLock.newCondition();
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private Bootstrap bootstrap;
    private EventLoopGroup workerGroup;

    private static Logger logger = LoggerFactory.getLogger(NettyHttpClientOutboundHandler.class);

    public NettyHttpClientOutboundHandler(String host, int port){

        this.host = host;
        this.port = port;
        //this.backendUrl = backendUrl.endsWith("/")?backendUrl.substring(0,backendUrl.length()-1):backendUrl;

        this.workerGroup = new NioEventLoopGroup();

        this.bootstrap = new Bootstrap();
        this.bootstrap.group(this.workerGroup);
        this.bootstrap.channel(NioSocketChannel.class);
        this.bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        this.bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                // 客户端接收到的是httpResponse响应，所以要使用HttpResponseDecoder进行解码
                ch.pipeline().addLast(new HttpResponseDecoder());

                //客户端发送的是httprequest，所以要使用HttpRequestEncoder进行编码
                ch.pipeline().addLast(new HttpRequestEncoder());
                ch.pipeline().addLast(new HttpObjectAggregator(512 * 1024));
                ch.pipeline().addLast(new HttpFillterOutboundHandler());
                //ch.pipeline().addLast(new HttpClientOutboundHandler());
            }
        });

    }

    public void connect() throws Exception {
        readLock.lock();

        try{
            if(!connected.get()){

                this.channelFuture = this.bootstrap.connect(this.host, this.port);
                this.channelFuture.sync();
                //连接成功，获取写锁，置标记位
                readLock.unlock();   //释放读锁，因为不允许读锁的升级
                writeLock.lock();
                try {
                    connected.compareAndSet(false,true);
                    //降级为读锁
                    readLock.lock();
                }finally {
                    //释放写锁
                    writeLock.unlock();
                }
            }
        } finally {
            readLock.unlock();
        }

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx)
            throws Exception {
        //置连接状态
        connected.compareAndSet(false,true);

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        FullHttpResponse endpointResponse = (FullHttpResponse) msg;

        byte[] body = endpointResponse.content().array();
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(body));
        response.headers().set("Content-Type", "application/json");
        response.headers().setInt("Content-Length", Integer.parseInt(endpointResponse.headers().get("Content-Length")));


        this.srcChannelHandlerContext.writeAndFlush(response);
    }


    public void handle(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx) {
        this.srcChannelHandlerContext = ctx;

        String route = httpEndpointRouter.route(Arrays.asList(fullRequest.uri()));
        String dstUrl = route.endsWith("/")?route.substring(0,route.length()-1):route + fullRequest.uri();

        FullHttpRequest interRequest = new DefaultFullHttpRequest(HTTP_1_1, fullRequest.method(),dstUrl, fullRequest.content());

        this.channelFuture.channel().writeAndFlush(interRequest);
    }
}