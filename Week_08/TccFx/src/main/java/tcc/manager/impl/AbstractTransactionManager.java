package tcc.manager.impl;

import request.Request;
import tcc.manager.TransactionManager;
import transaction.Transaction;
import transaction.TransactionProperties;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public class AbstractTransactionManager implements TransactionManager {

    //执行任务的线程池
    private ExecutorService executorService;

    //接收请求的单端阻塞队列
    private Queue<Request> requests = new ArrayBlockingQueue<>(100);

    public AbstractTransactionManager(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public Transaction buildTransactionFrom(Supplier<TransactionProperties> transactionPropertiesFactory) {
        TransactionProperties transactionProperties = transactionPropertiesFactory.get();
        return transactionProperties.initializeTransactionBuilder().build();
    }

    @Override
    public void startTccTx(Transaction transaction) {
        executorService.submit(transaction);
    }

    @Override
    public boolean take(Supplier<Request> requestFactory) {
        return requests.add(requestFactory.get());
    }

    @Override
    public void serve() {
        Request request = requests.poll();
        Transaction transaction = buildTransactionFrom(() -> request.getTransactionProperties());
        executorService.submit(transaction);
    }


}
