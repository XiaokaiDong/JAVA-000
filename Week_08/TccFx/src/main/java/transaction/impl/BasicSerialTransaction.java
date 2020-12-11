package transaction.impl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import request.Request;
import response.Response;
import tcc.service.TccService;
import transaction.Transaction;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 串行处理分布式事务，所以的参与方必须一次执行
 */
@Slf4j
@Data
public class BasicSerialTransaction implements Transaction {
    private String txID;

    private Request request;

    //需要cancel的任务数量
    private AtomicInteger cancelIndex = new AtomicInteger(0);

    //重试次数
    private AtomicInteger retryTime = new AtomicInteger(0);

    //最大重试次数
    private final static int maxRetryTime = 3;

    //是否允许各个参与的服务并发执行
    private boolean paralleled = false;

    //这里TCC事务涉及到的各个参与方是有顺序的，在builder中应该按照Transaction的定义按序添加TccService
    private List<TccService> participator;

    //执行TCC服务的栈
    private Deque<CompletableFuture<Void>> tasksStack = new ConcurrentLinkedDeque<>();

    //当前执行的TCC服务
    private AtomicInteger currTccService = new AtomicInteger(0);

    //执行任务的线程池
    private Executor executor;

    //需要在confirm阶段重试的参与方
    private Set<TccService> needReconfirmServices = new ConcurrentSkipListSet<>();

    //只允许用相应的Builder进行建造
    private BasicSerialTransaction() {}

    @Override
    public String getTxID() {
        return txID;
    }

    @Override
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void setContext(Request request) {
        this.request = request;
    }

    private void init() {
        for (TccService tccService: participator ) {
            tccService.tccStart(txID);
        }

    }

    /**
     * 执行这个事物
     */
    @Override
    public void run() {
        init();

        if (participator.size() == 0)
            return;

        tryIt();

        if (cancelIndex.get() > 0) {
            cancel();
        } else {
            while (maxRetryTime > 0 && !needReconfirmServices.isEmpty()) {
                confirm();
            }
        }

    }

    /**
     * try阶段
     */
    private void tryIt() {
        //使用栈进行
        CompletableFuture<Void> head = CompletableFuture
                .runAsync(() -> {
                    participator.get(currTccService.get()).tccTry(request);
                }, executor)
                .thenRunAsync(() -> {
                    this.needReconfirmServices.add(participator.get(currTccService.get()));
                }, executor)
                .exceptionally((e) -> {      //如果发生异常，则1、结束整个流程；2、cancel自己
                    log.info("The service [{}] failed because: {}",
                            participator.get(currTccService.get()).getName(),
                            e.getMessage());

                    //try发生异常，直接结束处理
                    currTccService.set(participator.size());
                    //cancel，记录需要cancel的任务数量，由取消线程统一处理
                    cancelIndex.set(currTccService.get());

                    return null;
                });

        //放入堆栈，开启流程
        tasksStack.push(head);

        CompletableFuture<Void> task = null;
        do {
            task = tasksStack.pop();
            if(currTccService.get() < participator.size()) {
                //取下一个分布式事务的参与方
                task.thenRunAsync(() -> {
                    participator.get(currTccService.incrementAndGet()).tccTry(request);
                }, executor)
                    //如果成功,则放入confirm集合
                    .thenRunAsync(() -> {
                        needReconfirmServices.add(participator.get(currTccService.get()));
                    }, executor)
                    .exceptionally((e) -> {
                        log.info("Try service [{}] failed because: {}",
                                participator.get(currTccService.get()).getName(),
                                e.getMessage());

                        //try发生异常，直接结束处理
                        currTccService.set(participator.size());
                        //cancel，记录需要cancel的任务数量，由取消线程统一处理
                        cancelIndex.set(currTccService.get());

                        return null;
                    });
                tasksStack.push(task);
            }

        } while(task != null);
    }

    private void cancel() {
        //进行cancel处理
        while (cancelIndex.get() > 0) {
            participator.get(cancelIndex.decrementAndGet()).tccCancel(request);
        }
    }

    private void confirm() {
        Iterator<TccService> it = needReconfirmServices.iterator();
        while(it.hasNext()) {
            TccService service = it.next();
            CompletableFuture
                    .runAsync(() -> {service.tccConfirm(request);})
                    .thenRunAsync(() -> needReconfirmServices.remove(service))
                    .exceptionally((e) -> {
                        log.info("the service confirm failed because {}, will retry for [{}] round",
                                e.getCause(), retryTime.get());
                        return null;
                    });
        }
        retryTime.incrementAndGet();
    }
}
