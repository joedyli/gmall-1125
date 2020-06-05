package com.atguigu.gmall.item.controller;

import java.io.IOException;
import java.util.concurrent.*;

public class ThreadDemo {

    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {

        CompletableFuture<String> Afuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("supplyAsync初始化子任务");
//            int i = 1 / 0;
            return "hello CompletableFuture.supplyAsync";
        });

        CompletableFuture<String> future1 = Afuture.thenApplyAsync(t -> {
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("上一个任务的返回结果1：" + t);
            return "hello thenApplyAsync1";
        });
        CompletableFuture<String> future2 = Afuture.thenApplyAsync(t -> {
            try {
                TimeUnit.SECONDS.sleep(4);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("上一个任务的返回结果2：" + t);
            return "hello thenApplyAsync2";
        });
        CompletableFuture<Void> future3 = Afuture.thenAcceptAsync(t -> {
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("上一个任务的返回结果3：" + t);
        });
        CompletableFuture<Void> future4 = Afuture.thenRunAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("不获取上一个任务的返回结果，也没有自己的返回结果");
        });
        CompletableFuture.allOf(future1, future2, future3, future4).join();
        System.out.println("主线程打印模拟return");

        System.in.read();

//                .whenCompleteAsync((t, u) -> { // 可以处理上一个任务的正常及异常结果集
//            System.out.println("开启另一个任务");
//            System.out.println("正常返回结果集t: " + t);
//            System.out.println("异常返回结果集u: " + u);
//        });

//        CompletableFuture.runAsync(() -> {
//            System.out.println("runAsync初始化子任务");
//        }).whenCompleteAsync((t, u) -> {
//            System.out.println("whenCompleteAsync执行另一个任务");
//        });

//        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(3, 5, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));
//        for (int i = 0; i < 500; i++) {
//            threadPoolExecutor.execute(() -> {
//                try {
//                    Thread.sleep(50);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.out.println("自定义线程池初始化了多线程程序！" + Thread.currentThread().getName());
//            });
//        }

//        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);
//        System.out.println(System.currentTimeMillis());
//        executorService.scheduleAtFixedRate(()->{
//            System.out.println("定时任务的线程池" + System.currentTimeMillis());
//        }, 5, 10, TimeUnit.SECONDS);
//        executorService.schedule(() -> {
//            System.out.println("定时任务的线程池" + System.currentTimeMillis());
//        }, 10, TimeUnit.SECONDS);

//        ExecutorService executorService = Executors.newFixedThreadPool(3);
//        ExecutorService executorService = Executors.newCachedThreadPool();
//        ExecutorService executorService = Executors.newSingleThreadExecutor();
//        for (int i = 0; i < 5; i++) {
//            Future<String> future = executorService.submit(() -> {
//                System.out.println("通过Executors工具类初始化了固定大小的线程池" + Thread.currentThread().getName());
//                return "hello executors";
//            });
            // 阻塞子线程
//            System.out.println(future.get());
//        }

//        try {
//            FutureTask<String> futureTask = new FutureTask<>(new MyCallable());
//            new Thread(futureTask).start();
//            while (!futureTask.isDone()){
//                System.out.println("你执行完了吗？！");
//            }
//            System.out.println(futureTask.get());
//        } catch (Exception e) {
//            System.out.println("子任务出现异常了");
//            e.printStackTrace();
//        }
//        new Thread(() -> {
//                System.out.println("通过Runnable接口初始化多线程程序！（lamboda表达式）");
//            }).start();
//        new Thread(new MyRunnable()).start();
//        new MyThread().start();
    }
}
class MyCallable implements Callable<String>{
    @Override
    public String call() throws Exception {
        System.out.println("通过Callable接口初始化多线程程序！");
//        int i = 1/0;
        return "hello callable!";
    }
}
class MyRunnable implements Runnable{
    @Override
    public void run() {
        System.out.println("通过Runnable接口初始化多线程程序！（实现类）");
    }
}
class MyThread extends Thread{
    @Override
    public void run() {
        System.out.println("通过Thread抽象类初始化多线程程序！");
    }
}
