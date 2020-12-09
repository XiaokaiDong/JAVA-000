package io.github.kimmking.gateway.inbound;

import io.github.kimmking.gateway.outbound.netty4.NettyHttpClientOutboundHandler;
import io.github.kimmking.gateway.outbound.netty4.SetSrcUrlHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class HttpInboundInitializer extends ChannelInitializer<SocketChannel> {
	
	private String proxyServer;
	
	public HttpInboundInitializer(String proxyServer) {
		this.proxyServer = proxyServer;
	}
	
	@Override
	public void initChannel(SocketChannel ch) {
		ChannelPipeline p = ch.pipeline();
//		if (sslCtx != null) {
//			p.addLast(sslCtx.newHandler(ch.alloc()));
//		}
		p.addLast(new HttpServerCodec());
		//p.addLast(new HttpServerExpectContinueHandler());
		p.addLast(new HttpObjectAggregator(1024 * 1024));
		//p.addLast(new HttpInboundHandler(this.proxyServer));
		p.addLast(new SetSrcUrlHandler());
		p.addLast(new NettyHttpClientOutboundHandler());
	}
}
