package io.kimmking.rpcfx.client.channel;

import com.alibaba.fastjson.JSON;
import io.kimmking.rpcfx.api.RpcfxRequest;
import io.kimmking.rpcfx.api.RpcfxResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * NETTY HTTP客户端
 */
public class NettyHttpClient {

    //线程组
    private final EventLoopGroup group = new NioEventLoopGroup();

    //启动类
    private final Bootstrap bootstrap = new Bootstrap();

    //目标URL
    private URL url;

    private ChannelFuture channelFuture;

    public void start(URL url) throws InterruptedException {
        this.url = url;
        try {
            String host = url.getHost();
            int port = url.getPort();
            bootstrap.group(group)
                    .remoteAddress(new InetSocketAddress(host, port))
                    //长连接
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        protected void initChannel(Channel channel) throws Exception {

                            //包含编码器和解码器
                            channel.pipeline().addLast(new HttpClientCodec());

                            //聚合
                            channel.pipeline().addLast(new HttpObjectAggregator(32 * 1024));

                            //解压
                            channel.pipeline().addLast(new HttpContentDecompressor());

                            //channel.pipeline().addLast(new ClientHandler());
                        }
                    });

            channelFuture = bootstrap.connect().sync();

            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    public RpcfxResponse post(RpcfxRequest req){
        String reqJson = JSON.toJSONString(req);
        System.out.println("req json: "+reqJson);

        FullHttpRequest request = null;
        try {
            request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.POST,
                    url.toString(),
                    Unpooled.wrappedBuffer(reqJson.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        request.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8")
                //开启长连接
                .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                //设置传递请求内容的长度
                .set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());

        try {
            channelFuture.channel().write(request).sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}

