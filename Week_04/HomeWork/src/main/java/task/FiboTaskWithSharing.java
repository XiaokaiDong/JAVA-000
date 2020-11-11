package task;

public class FiboTaskWithSharing implements Runnable{
    private final int target;
    private volatile Result result;

    public FiboTaskWithSharing(int target, Result result) {
        this.target = target;
        this.result = result;
    }

    @Override
    public void run() {
        fibo();
    }

    private  void fibo() {

        if (target < 2) {
            result.setResult(1);
            return;
        }

        int f1 = 1;
        int f2 = 1;
        int i = 2;
        while(i++ <= target){
            f2 = f1 + f2;
            f1 = f2 - f1;
        }
        result.setResult(f2);
    }
}
