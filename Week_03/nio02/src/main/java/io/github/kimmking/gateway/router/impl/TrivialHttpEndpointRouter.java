package io.github.kimmking.gateway.router.impl;

import io.github.kimmking.gateway.router.HttpEndpointRouter;

import java.util.List;

public class TrivialHttpEndpointRouter implements HttpEndpointRouter {
    @Override
    public String route(List<String> endpoints) {
        return "http://localhost:8080";
    }
}
