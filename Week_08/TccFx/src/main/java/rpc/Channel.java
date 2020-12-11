package rpc;

import java.util.concurrent.Future;

public interface Channel {
    <T, R> R invokeSync(T content, long timeout);
    <T, R> Future<R> invoke(T content, long timeout);
}
