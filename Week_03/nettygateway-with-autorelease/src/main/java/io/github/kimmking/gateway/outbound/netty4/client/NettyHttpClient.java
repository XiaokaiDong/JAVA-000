package io.github.kimmking.gateway.outbound.netty4.client;

import io.github.kimmking.gateway.router.impl.TrivialHttpEndpointRouter;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * NETTY HTTP客户端
 */
public class NettyHttpClient {

    private TrivialHttpEndpointRouter router;

    private NettyConnection connection = new NettyConnection();

    public NettyHttpClient(TrivialHttpEndpointRouter router) {
        this.router = router;
    }

    public void handle(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx)  {

        String uri = router.route(router.getEndpoints(fullRequest.uri()));

        URL url = null;
        try {
            url = new URL(uri);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        //此处fullRequest经过两个流水线，会被释放两次，所以需要增加一次计数，以供客户端进行释放
        //这样可以减少不必要的内存拷贝
        ReferenceCountUtil.retain(fullRequest);

        fullRequest.setUri(url.getPath());
        fullRequest.headers()
                .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                .set(HttpHeaderNames.HOST, url.getHost() + ":" + url.getPort());

        if (url.getHost() == null) {
            System.out.println("null host!");
        }

        doHandle(fullRequest, ctx, url.getHost(), url.getPort());

    }

    /**
     * 执行HTTP请求
     * @param fullRequest  请求
     * @param ctx          用于返回相应
     * @param host         目标主机
     * @param port         端口
     */
    private void doHandle(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx,
                                        String host, int port) {
        ChannelPromise promise = null;
        try {
            promise = connection.sendMessageByHttp(fullRequest,host, port);
            //异步转同步
            promise.get(10, TimeUnit.SECONDS);
            //System.out.println("发送响应...");
            Channel targetChannel = promise.channel();
            ClientHandler handler = (ClientHandler)targetChannel.pipeline().get(NettyConnectionPool.CLIENT_HANDLER);

            if (fullRequest != null) {
                if (!HttpUtil.isKeepAlive(fullRequest)) {
                    ctx.write(handler.getResponse()).addListener(ChannelFutureListener.CLOSE);
                } else {
                    ctx.write(handler.getResponse());
                }
            }
            ctx.flush();
            connection.makeUsable(targetChannel);

            //测试直接归还连接
            //System.out.println("归还连接...");
            //不再连接池中的时候，才归还到连接池中
            if (!targetChannel.attr(NettyConnectionPool.IN_THE_POOL).get())
                NettyConnectionPool.getInstance().releaseChannel(targetChannel);

//            if (targetChannel.attr(NettyConnectionPool.OWNER_KEY).get() == null) {
//                System.out.println("归还连接...");
//                NettyConnectionPool.getInstance().releaseChannel(targetChannel);
//            }

        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            System.out.println("发生异常： " + e.getMessage());
            //返回一个500错误
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.GATEWAY_TIMEOUT);
            ctx.writeAndFlush(response);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (connection != null)
            connection.release();
    }
}

