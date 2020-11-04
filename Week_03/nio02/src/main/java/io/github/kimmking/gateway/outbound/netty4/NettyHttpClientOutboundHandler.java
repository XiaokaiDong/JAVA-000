package io.github.kimmking.gateway.outbound.netty4;

import io.github.kimmking.gateway.inbound.HttpInboundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class NettyHttpClientOutboundHandler  extends ChannelInboundHandlerAdapter {

    private  String host;
    private int port;
    private String backendUrl;
    private FullHttpRequest fullRequest;

    private AtomicBoolean connected = new AtomicBoolean(false);
    private Bootstrap bootstrap;
    private EventLoopGroup workerGroup;
    private ChannelHandlerContext sourceCtx;
    private ChannelFuture connectedChannelFuture;

    private static Logger logger = LoggerFactory.getLogger(NettyHttpClientOutboundHandler.class);

    public NettyHttpClientOutboundHandler(String host, int port, String backendUrl){

        this.host = host;
        this.port = port;
        this.backendUrl = backendUrl.endsWith("/")?backendUrl.substring(0,backendUrl.length()-1):backendUrl;
        this.sourceCtx = null;
        this.connectedChannelFuture = null;

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
                ch.pipeline().addLast(new HttpFillterOutboundHandler());
                //客户端发送的是httprequest，所以要使用HttpRequestEncoder进行编码
                ch.pipeline().addLast(new HttpRequestEncoder());
                //ch.pipeline().addLast(new HttpClientOutboundHandler());
            }
        });
//        try{
//            connect();
//        } catch (Exception e){
//            e.printStackTrace();
//        }
    }

    public void connect() throws Exception {
        try {
            // Start the client.
            this.connectedChannelFuture = this.bootstrap.connect(this.host, this.port).sync();
            // Wait until the connection is closed.
            this.connectedChannelFuture.channel().closeFuture().sync();
        } finally {
            this.workerGroup.shutdownGracefully();
        }

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx)
            throws Exception {
        logger.info("连接后端服务[{}:{}]成功", host, port);
        //发送数据
        this.fullRequest.setUri(backendUrl +  fullRequest.uri());
        ctx.writeAndFlush(this.fullRequest);

    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        FullHttpResponse response = (FullHttpResponse) msg;
        this.sourceCtx.write(response);
        
    }

//    public void handle(final FullHttpRequest fullRequest) {
//        handle(fullRequest, this.sourceCtx);
//    }

    public void handle(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx) {
        this.sourceCtx = ctx;
        this.fullRequest = fullRequest;
        try{
            connect();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}