package tcc.service;

import lombok.extern.slf4j.Slf4j;
import rpc.Channel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Slf4j
public class AbstractTccService<T,R> implements TccService<T,R>{
    private String name;

    //事务的ID
    private String txId;

    //底层的通道
    private Channel<T,R> channel;

    //交易状态
    private ServiceStatus serviceStatus;

    //超时时间
    private long timeOut;

    //重试次数
    private int redoTimes;

    @Override
    public CompletableFuture<R> tccTry(T context) {

        CompletableFuture<R> doTry = CompletableFuture.supplyAsync(
                () -> channel.invoke(context)
        );

        return doTry;
    }

    @Override
    public CompletableFuture<R> tccConfirm(T context) {
        CompletableFuture<R> doTry = CompletableFuture.supplyAsync(
                () -> channel.invoke(context)
        );

        return doTry;
    }

    @Override
    public CompletableFuture<R> tccCancel(T context) {
        CompletableFuture<R> doTry = CompletableFuture.supplyAsync(
                () -> channel.invoke(context)
        );

        return doTry;
    }

    @Override
    public void tccStart(String txId) {
        this.txId = txId;
        serviceStatus = ServiceStatus.STARTED;

    }
}
