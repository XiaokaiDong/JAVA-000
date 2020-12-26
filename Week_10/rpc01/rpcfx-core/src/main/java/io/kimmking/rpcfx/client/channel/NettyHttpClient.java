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
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * NETTY HTTP客户端
 */
public class NettyHttpClient {

    /**
     * 利用Netty以POST、同步的方式发送HTTP请求
     * @param req 请求
     * @param url 目标url
     * @return RpcfxResponse
     * @throws InterruptedException
     */
    public static RpcfxResponse post(RpcfxRequest req, URL url) throws InterruptedException {
        String reqJson = JSON.toJSONString(req);
        System.out.println("req json: "+reqJson);

        FullHttpRequest request = null;
        try {
            request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.POST,
                    url.getPath(),
                    Unpooled.wrappedBuffer(reqJson.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        request.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8")
                //开启长连接
                .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                //设置传递请求内容的长度
                .set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
                .set(HttpHeaderNames.HOST, url.getHost() + ":" + url.getPort());

        NettyConnection connection = new NettyConnection();

        FullHttpResponse httpResponse = connection.sendMessageByHttpSync(request, url.getHost(), url.getPort());

        connection.close();

        String respJson = null;
        try {
            int cnt = httpResponse.refCnt();
            respJson = httpResponse.content().toString(CharsetUtil.UTF_8);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        System.out.println("resp json: "+respJson);

        RpcfxResponse result = JSON.parseObject(respJson, RpcfxResponse.class);
        ReferenceCountUtil.release(httpResponse);
        return result;


    }

}

