package io.github.kimmking.gateway.outbound.netty4;

import io.github.kimmking.gateway.outbound.netty4.common.ChannelProperties;
import io.github.kimmking.gateway.router.HttpEndpointRouter;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.List;

public class SetSrcUrlHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private String srcUrl;


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        Channel channel = ctx.channel();
        channel.attr(ChannelProperties.SRC_URL.getAttributeKey()).set(msg.uri());
    }
}
