//package tcc.service;
//
//import lombok.extern.slf4j.Slf4j;
//import rpc.Channel;
//
//import java.util.concurrent.Future;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//@Slf4j
//public class AbstractTccService implements TccService{
//    private final String name;
//
//    //事务的ID
//    private String txId;
//
//    //底层的通道
//    private Channel channel;
//
//    //交易状态
//    private ServiceStatus serviceStatus;
//
//    //超时时间
//    private long timeOut;
//
//    //是否已经启动
//    AtomicBoolean started = new AtomicBoolean(false);
//
//    public AbstractTccService(String name, Channel channel, long timeOut) {
//        this.name = name;
//        this.channel = channel;
//        this.timeOut = timeOut;
//    }
//
//    public <T, R> Future<R> tccTry(T tryContext) {
//        serviceStatus = ServiceStatus.TRYING;
//        return channel.invoke(tryContext, timeOut);
//    }
//
//    @Override
//    public <T, R> Future<R> tccConfirm(T confirmContext) {
//        serviceStatus = ServiceStatus.CONFIRMING;
//        return channel.invoke(confirmContext, timeOut);
//    }
//
//    @Override
//    public <T, R> Future<R> tccCancel(T cancelContext) {
//        serviceStatus = ServiceStatus.CANCELLING;
//
//        return channel.invoke(cancelContext, timeOut);
//    }
//
//    /**
//     * 获取事务ID
//     * @param txId，事务ID
//     */
//    @Override
//    public void tccStart(String txId) {
//        synchronized (this) {
//            if (!started.get()) {
//                this.txId = txId;
//                serviceStatus = ServiceStatus.STARTED;
//                started.compareAndSet(false, true);
//            }
//        }
//    }
//
//    public ServiceStatus getServiceStatus() {
//        return serviceStatus;
//    }
//
//    public void setServiceStatus(ServiceStatus serviceStatus) {
//        this.serviceStatus = serviceStatus;
//    }
//}
