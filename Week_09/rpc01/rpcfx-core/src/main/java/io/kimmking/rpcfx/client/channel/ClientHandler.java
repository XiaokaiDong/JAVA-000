package io.kimmking.rpcfx.client.channel;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.Data;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 客户端处理器
 */
@Data
public class ClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private ChannelHandlerContext ctx;
    private ChannelPromise promise;
    private CountDownLatch connected;

    private FullHttpResponse response;

    public ClientHandler() {
        super();
        connected = new CountDownLatch(1);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        response = msg;
        //增加引用计数，以防止被释放
        ReferenceCountUtil.retain(response);
        promise.setSuccess();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.ctx = ctx;
        //标记此通道已连接
        connected.countDown();
    }

    /**
     * 发送请求，默认超时时间是30S，目前先写死这个时间
     * @param request 请求
     * @return 一个ChannelPromise，用于获取结果
     * @throws InterruptedException
     */
    public ChannelPromise sendMessage(FullHttpRequest request) throws InterruptedException {
        connected.await(30, TimeUnit.SECONDS);
        promise = ctx.newPromise();
        ctx.writeAndFlush(request);
        return promise;
    }

    public FullHttpResponse getResponse() {
        return response;
    }
}

