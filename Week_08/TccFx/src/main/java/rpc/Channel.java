package rpc;

import java.util.concurrent.Future;

public interface Channel<T,R> {
    R invoke(T content);
}
