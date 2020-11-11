package task;

import java.util.concurrent.Callable;

public class FiboTaskCallable implements Callable <Integer> {
    private final int target;

    public FiboTaskCallable(int target) {
        this.target = target;
    }

    @Override
    public Integer call() throws Exception {
        return fibo();
    }

    private int fibo() {
        int result = 0;
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

        return result;
    }
}
