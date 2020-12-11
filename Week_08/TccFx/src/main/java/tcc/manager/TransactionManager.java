package tcc.manager;

import request.Request;
import transaction.Transaction;
import transaction.TransactionProperties;

import java.util.function.Supplier;

public interface TransactionManager{

    /**
     * 建造一个交易
     * @param transactionPropertiesFactory 交易定义
     * @return 返回一个交易
     */
    Transaction buildTransactionFrom(Supplier<TransactionProperties> transactionPropertiesFactory);

    /**
     * 将交易Transaction放入线程池（看做一个队列）
     * @param transaction 某个具体的交易
     */
    void startTccTx(Transaction transaction);

    /**
     * 接收请求
     */
    boolean take(Supplier<Request> requestFactory);

    /**
     * 开启服务
     */
    void serve();
}
