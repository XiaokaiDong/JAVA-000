package io.github.kimmking.gateway.outbound.netty4;

import io.github.kimmking.gateway.filter.HttpRequestFilter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequestEncoder;

public class HttpFillterOutboundHandler extends HttpRequestEncoder implements HttpRequestFilter {
    @Override
    public void filter(FullHttpRequest fullRequest, ChannelHandlerContext ctx) {
        fullRequest.headers().set("nio","Dong Xiaokai");
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        FullHttpRequest fullRequest = (FullHttpRequest)msg;
        filter(fullRequest,ctx);
        ctx.write(fullRequest);
    }
}
