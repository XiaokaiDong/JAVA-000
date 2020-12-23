package io.github.kimmking.gateway.outbound.netty4.client;

import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.ReferenceCountUtil;
import lombok.Data;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 客户端处理器
 */
@Data
public class ClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private ChannelHandlerContext ctx = null;
    private ChannelPromise promise;
    //private CountDownLatch connected;

    private FullHttpResponse response;

//    public ClientHandler() {
//        super();
//        connected = new CountDownLatch(1);
//    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        response = msg;
        //System.out.println("channel请求已完成：" + ctx.channel().id());
        //增加引用计数，以防止被释放
        ReferenceCountUtil.retain(response);
        if (promise.isSuccess())
            return;
        promise.setSuccess();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    /**
     * @param request 请求
     * @return 一个ChannelPromise，用于获取结果
     * @throws InterruptedException
     */
    public ChannelPromise sendMessage(FullHttpRequest request) throws InterruptedException {
        //设置对应的channel为使用状态
        ctx.channel().attr(NettyConnectionPool.IN_USE_ATTR_KEY).set(true);
        promise = ctx.newPromise();
        ctx.writeAndFlush(request);
        return promise;
    }

    public FullHttpResponse getResponse() {
        return response;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //super.exceptionCaught(ctx, cause);
        System.out.println("捕获异常：" + cause.getMessage());
        System.out.println("断开连接...");
        ctx.close();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        this.ctx = ctx;
    }
}

