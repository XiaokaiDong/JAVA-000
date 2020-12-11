package transaction;

import request.Request;

import java.util.concurrent.Executor;

/**
 * 定义一个交易，注意，这里Transaction表示交易，而不是事务
 */
public interface Transaction extends Runnable{
    /**
     * 返回事务ID
     * @return 事务ID
     */
    String getTxID();

    /**
     * 设置线程池，实现事物之间的隔离
     * @param executor 线程池
     */
    void setExecutor(Executor executor);

    /**
     * 设置事务的上下文，避免在TCC服务间频繁传递参数
     * @param request
     */
    void setContext(Request request);

}
