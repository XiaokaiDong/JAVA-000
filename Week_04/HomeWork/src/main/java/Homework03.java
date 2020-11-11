import task.*;

import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * 本周作业：（必做）思考有多少种方式，在main函数启动一个新线程或线程池，
 * 异步运行一个方法，拿到这个方法的返回值后，退出主线程？
 * 写出你的方法，越多越好，提交到github。
 *
 * 一个简单的代码参考：
 */
public class Homework03 {

    public static void main(String[] args) {

        long start=System.currentTimeMillis();
        // 在这里创建一个线程或线程池，
        // 异步执行 下面方法

        //================================================================

        //1、使用synchronized-wait-notify
        FiboTaskWithNotifying fiboTaskWithNotifying = new FiboTaskWithNotifying(36);

        Thread worker = new Thread(fiboTaskWithNotifying, "notifying-worker");
        worker.start();

        int result = 0;

        //1、使用wait-notifyAll
        try {
            result = fiboTaskWithNotifying.getResult();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("[wait-notifyAll]异步计算结果为："+result);

        //==================================================================

        //2、使用join

        try {
            worker.join();
            result = fiboTaskWithNotifying.getResult();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("[使用join]异步计算结果为："+result);

        //=======================================================================

        //3、使用condition
        FiboTaskWithLock fiboTaskWithLock = new FiboTaskWithLock(36);
        worker = new Thread(fiboTaskWithLock, "lock-worker");
        worker.start();
        try {
            //Thread.sleep(100);
            result = fiboTaskWithLock.getResult();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("[condition]异步计算结果为："+result);

        //========================================================================

        //4、使用线程池，提交一个Callable

        ExecutorService fiboService = Executors.newSingleThreadExecutor();
        FiboTaskCallable fiboTaskCallable = new FiboTaskCallable(36);

        try {
            result = fiboService.submit(fiboTaskCallable).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        System.out.println("[SingleThreadExecutor]异步计算结果为："+result);

        //======================================================================

        //5、使用线程池，提交一个Runnable，使用wait-notify或Lock + Condition获取结果

        FiboTaskWithNotifying fiboTaskWithNotifying1 = new FiboTaskWithNotifying(36);
        fiboService.submit(fiboTaskWithNotifying1);

        try {
            result = fiboTaskWithNotifying.getResult();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("[SingleThreadExecutor + wait-notify]异步计算结果为："+result);

        //===========================================================================

        //6、使用线程池，提交一个Runnable，然后使用future获取结果
        Result result2 = new Result();
        FiboTaskWithSharing fiboTaskWithSharing = new FiboTaskWithSharing(36, result2);

        Future<Result> future = fiboService.submit(fiboTaskWithSharing,result2);

        Result result3 = null;

        try {
            result3 = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        System.out.println("[SingleThreadExecutor + Future<result>]异步计算结果为："+ result3.getResult());

        //===========================================================================

        //7、使用CountDownLatch
        result2.setResult(0);
        CountDownLatch latch = new CountDownLatch(1);

        fiboService.submit(() -> {
            result2.setResult(FiboTask.fibo(36));
            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("[CountDownLatch]异步计算结果为："+ result2.getResult());

        //===============================================
        //8、使用CyclicBarrier
        result2.setResult(0);
        CyclicBarrier cyclicBarrier = new CyclicBarrier(1,()->{
            System.out.println("[CyclicBarrier]异步计算结果为："+ result2.getResult());
        });

        fiboService.submit(() -> {
            result2.setResult(FiboTask.fibo(36));
            try {
                cyclicBarrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        });

        //===============================================
        //9、使用LockSupport
        result2.setResult(0);

        Thread mainThread = Thread.currentThread();

        fiboService.submit(() -> {
            result2.setResult(FiboTask.fibo(36));
            LockSupport.unpark(mainThread);
        });

        LockSupport.park();
        System.out.println("[LockSupport]异步计算结果为："+ result2.getResult());

        //===============================================
        //10、CompletableFuture
        result = 0;
        result = CompletableFuture.supplyAsync( () -> {
            return FiboTask.fibo(36);
        }).join();
        System.out.println("[CompletableFuture]异步计算结果为："+ result);

        //===================================================

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("使用时间："+ (System.currentTimeMillis()-start) + " ms");

        // 然后退出main线程
        fiboService.shutdown();
    }



}