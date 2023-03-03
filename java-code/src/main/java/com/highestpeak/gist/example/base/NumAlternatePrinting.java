package com.highestpeak.gist.example.base;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author zhangjike <zhangjike03@kuaishou.com>
 * Created on 2022-09-08
 */
public class NumAlternatePrinting {
    public static void main(String[] args) {
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        AtomicInteger iter = new AtomicInteger(0);
        Runnable runnable = () -> {
            while (iter.get() < 30) {
                lock.lock();
                try {
                    System.out.println("i am in."+ " ;" + Thread.currentThread().getName());
                    condition.signalAll();
                    int i = iter.incrementAndGet();
                    System.out.println("i: " + i + " ;" + Thread.currentThread().getName());
                    condition.await();
                } catch (InterruptedException e) {
                    System.out.println("exception." + Thread.currentThread().getName());
                } finally {
                    lock.unlock();
                }
            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            executorService.execute(runnable);
        }

        System.out.println("have a nice day.");
    }
}
