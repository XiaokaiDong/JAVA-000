package task;

public class FiboTaskWithNotifying implements Runnable {
    private final int target;
    private int result = 1;
    private volatile boolean done = false;

    public FiboTaskWithNotifying(int target) {
        this.target = target;
    }

    @Override
    public void run() {
        fibo();
    }

    private synchronized void fibo() {

        if (target < 2) {
            result = 1;
            done = true;
            notifyAll();
            return;
        }

        int f1 = 1;
        int f2 = 1;
        int i = 2;
        while(i++ <= target){
            f2 = f1 + f2;
            f1 = f2 - f1;
        }
        result = f2;
        done = true;
        notifyAll();
    }

    public synchronized int getResult() throws InterruptedException {
        while(!done) {
            wait();
        }
        return result;
    }
}
