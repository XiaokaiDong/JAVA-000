package io.github.kimmking.gateway.router.impl;

import io.github.kimmking.gateway.router.GetHttpEndpoints;
import io.github.kimmking.gateway.router.HttpEndpointRouter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TrivialHttpEndpointRouter implements HttpEndpointRouter, GetHttpEndpoints {
    @Override
    public String route(List<String> endpoints) {
        return "http://localhost:8808/test";
    }

    @Override
    public List<String> getEndpoints(String url){
        List result = new ArrayList<>();
        result.add(url);
        return result;
    }
}
