package task;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FiboTaskWithLock implements Runnable {
    private final int target;
    private int result = 1;
    private volatile boolean done = false;
    private final Lock lock = new ReentrantLock();
    private final Condition completed = lock.newCondition();

    public FiboTaskWithLock(int target) {
        this.target = target;
    }

    @Override
    public void run() {
        fibo();
    }

    private void fibo() {

        lock.lock();

        try {


            if (target < 2) {
                result = 1;
            }else {
                int f1 = 1;
                int f2 = 1;
                int i = 2;
                while(i++ <= target){
                    f2 = f1 + f2;
                    f1 = f2 - f1;
                }
                result = f2;
            }

            done = true;
            completed.signalAll();
        }finally {
            lock.unlock();
        }
    }

    public int getResult() throws InterruptedException {
        lock.lock();
        try {
            while(!done) {
                completed.await();
            }
        }finally {
            lock.unlock();
        }
        return result;
    }
}
