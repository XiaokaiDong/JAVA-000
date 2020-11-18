package io.github.kimmking.gateway.outbound.netty4.dispatcher;

import io.netty.handler.codec.http.HttpResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestPendingCenter {
    private Map<Long, OperationResultFuture> map = new ConcurrentHashMap<>();

    public void add(Long streamId, OperationResultFuture future){
        this.map.put(streamId, future);
    }

    public void set(Long streamId, HttpResponse response){
        OperationResultFuture operationResultFuture = this.map.get(streamId);
        if (operationResultFuture != null) {
            operationResultFuture.setSuccess(response);
            this.map.remove(streamId);
        }
    }
}
