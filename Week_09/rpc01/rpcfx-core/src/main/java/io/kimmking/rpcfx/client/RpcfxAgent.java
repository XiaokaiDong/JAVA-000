package io.kimmking.rpcfx.client;

import com.alibaba.fastjson.JSON;
import io.kimmking.rpcfx.api.RpcfxRequest;
import io.kimmking.rpcfx.api.RpcfxResponse;
import io.kimmking.rpcfx.client.aop.annotation.RpcfxService;
import net.bytebuddy.implementation.bind.annotation.*;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class RpcfxAgent {
    public static final MediaType JSONTYPE = MediaType.get("application/json; charset=utf-8");

    @RuntimeType
    public Object intercept(@Origin Method method, @AllArguments Object[] args, @Super Object parent) throws Exception {
        RpcfxRequest request = new RpcfxRequest();
        request.setServiceClass(parent.getClass().getName());
        request.setMethod(method.getName());
        request.setParams(args);

        RpcfxService rpcfxService = method.getAnnotation(RpcfxService.class);
        String url = rpcfxService.url();

        if (url == null) {
            System.out.println("Cannot find the url the service corresponding to!");
            throw new Exception("missing Url on service!");
        } else {
            RpcfxResponse response = post(request, url);

            return JSON.parse(response.getResult().toString());
        }
    }

    private RpcfxResponse post(RpcfxRequest req, String url) throws IOException {
        String reqJson = JSON.toJSONString(req);
        System.out.println("req json: "+reqJson);

        // 1.可以复用client
        // 2.尝试使用httpclient或者netty client
        OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(JSONTYPE, reqJson))
                .build();
        String respJson = client.newCall(request).execute().body().string();
        System.out.println("resp json: "+respJson);
        return JSON.parseObject(respJson, RpcfxResponse.class);
    }
}
