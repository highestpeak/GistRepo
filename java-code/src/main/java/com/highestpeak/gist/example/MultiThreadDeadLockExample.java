package com.highestpeak.gist.example;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-15
 */
public class MultiThreadDeadLockExample {

    public static void deadLockExample1() {
        //noinspection unchecked
        final ArrayBlockingQueue<String>[] resources = new ArrayBlockingQueue[]{
                new ArrayBlockingQueue<String>(1),
                new ArrayBlockingQueue<String>(1),
        };
        resources[0].add("resource 1");
        resources[1].add("resource 2");

        Object lock = new Object();
        new Thread(() -> {
            synchronized (lock) {
                try {
                    String take = resources[0].take();
                    System.out.println(take);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                lock.notify();
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    String take = resources[1].take();
                    System.out.println(take);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }, "thread1").start();
        new Thread(() -> {
            synchronized (lock) {
                try {
                    String take = resources[1].take();
                    System.out.println(take);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                lock.notify();
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    String take = resources[0].take();
                    System.out.println(take);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "thread2").start();
    }

    public static void deadLockExample2() {
        final Object a = new Object();
        final Object b = new Object();
        Thread threadA = new Thread(() -> {
            synchronized (a) {
                try {
                    System.out.println("now i in threadA-lock a");
                    Thread.sleep(1000L);
                    synchronized (b) {
                        System.out.println("now i in threadA-lock b");
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        });

        Thread threadB = new Thread(() -> {
            synchronized (b) {
                try {
                    System.out.println("now i in threadB-lock b");
                    Thread.sleep(1000L);
                    synchronized (a) {
                        System.out.println("now i in threadB-lock a");
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        });

        threadA.start();
        threadB.start();
    }

}
