package io.github.kimmking.gateway.outbound.netty4;

import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyHttpClientOutboundHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private NettyHttpClientOutboundHandlerHelper handlerHelper;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        handlerHelper = new NettyHttpClientOutboundHandlerHelper(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws Exception {
        handlerHelper.handle(httpRequest);
    }


}