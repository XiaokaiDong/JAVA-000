package tcc.service;

import java.util.Map;
import java.util.concurrent.Future;

public interface TccService<T,R> {
    Future<R> tccTry(T context);
    Future<R> tccConfirm(T context);
    Future<R>  tccCancel(T context);
    void tccStart(String txId);
}
