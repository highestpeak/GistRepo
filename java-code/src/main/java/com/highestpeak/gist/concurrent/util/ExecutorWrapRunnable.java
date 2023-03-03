package com.highestpeak.gist.concurrent.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zhangjike <zhangjike03@kuaishou.com>
 * Created on 2021-08-17
 */
public class ExecutorWrapRunnable {

    /**
     * eg: when start a new executor then send a http to a report receiver
     */
    private static class ReportRunnable implements Runnable {

        private Runnable anotherRunnable;

        public ReportRunnable(Runnable anotherRunnable) {
            this.anotherRunnable = anotherRunnable;
        }

        @Override
        public void run() {
            // do some thing
            anotherRunnable.run();
            System.out.println("在这里做一些上报的事情");
        }
    }

    /**
     * sth like ktrace
     */
    private static class TraceRunnable implements Runnable {

        private Runnable anotherRunnable;

        public TraceRunnable(Runnable anotherRunnable) {
            this.anotherRunnable = anotherRunnable;
        }

        @Override
        public void run() {
            // do some thing
            anotherRunnable.run();
            System.out.println("在这里生成/注入线程跟踪id");
        }
    }

    public static void main(String[] args) {
        int coreSize = 10;
        // maxSize某种意义上对应queueSize
        int maxSize = 100;
        long keepAliveSeconds = 1;
        // 注意这个queue的大小的设置!
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(Math.max(coreSize, maxSize));
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                // thread.setName();
                // thread.setUncaughtExceptionHandler();
                // thread.setDaemon();
                // thread.setPriority();
                return thread;
            }
        };

        ThreadPoolExecutor executor = new ThreadPoolExecutor(coreSize, maxSize, keepAliveSeconds, TimeUnit.SECONDS, workQueue, threadFactory) {
            @Override
            public void execute(Runnable command) {
                // do some thing
                ReportRunnable reportWrapCommand = new ReportRunnable(command);
                TraceRunnable traceWrapCommand = new TraceRunnable(reportWrapCommand);
                // execute wrap command
                super.execute(traceWrapCommand);
            }
        };
    }
}
