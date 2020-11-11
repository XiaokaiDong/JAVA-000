package task;

import java.util.concurrent.CountDownLatch;

public class FiboTask {

    public static int fibo(int target) {
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
