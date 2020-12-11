package tcc.service;

import java.util.concurrent.Future;

public interface TccService {
    <T> void tccTry(T tryContext);
    <T> void tccConfirm(T confirmContext);
    <T> void tccCancel(T cancelContext);

    /**
     * 初始化一个服务，初始化需要将这个服务的状态等相关信息序列化，比如序列化到数据库中或者消息队列中
     * @param txId，所属交易的ID
     */
    void tccStart(String txId);
    String getName();
}
