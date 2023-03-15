package com.highestpeak.gist.example;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-15
 */
@SuppressWarnings("unused")
public class MultiThreadAlternatePrinting {

    public static void main(String[] args) {

    }

    /**
     * 两个线程交替打印
     */
    public static void twoPrint() throws InterruptedException {
        final int[] i = new int[]{0};
        Object lock = new Object();

        Runnable printTask = () -> {
            synchronized (lock) {
                while (i[0] < 26) {
                    System.out.println(Thread.currentThread().getName() + " " + (char) ('a' + i[0]));
                    i[0]++;
                    lock.notify();
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                lock.notify();
            }
        };

        Thread thread1 = new Thread(printTask, "thread1");
        Thread thread2 = new Thread(printTask, "thread2");
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
    }

    /**
     * 开启三个线程，这三个线程的 ID 分别是 A、B 和 C，每个线程把自己的 ID 在屏幕上打印 10 遍，要求输出结果必须按 ABC 的顺序显示，如 ABCABCABC
     */
    public static void threePrint() throws InterruptedException {
        final int[] i = new int[]{0};
        final String[] threadName = new String[]{"thread1", "thread2", "thread3"};
        Object lock = new Object();
        Runnable printTask = () -> {
            String currName = Thread.currentThread().getName();
            synchronized (lock) {
                while (i[0] < 26) {
                    try {
                        while (!threadName[i[0] % 3].equals(currName)) {
                            lock.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (i[0] <= 25) {
                        System.out.println(currName + " " + (char) ('a' + i[0]));
                    }
                    i[0]++;
                    lock.notifyAll();
                }
                lock.notify();
            }
        };
        Thread thread1 = new Thread(printTask, "thread1");
        Thread thread2 = new Thread(printTask, "thread2");
        Thread thread3 = new Thread(printTask, "thread3");
        thread1.start();
        thread2.start();
        thread3.start();
        thread1.join();
        thread2.join();
        thread3.join();
    }

    public static void threePrintUseLockIter() {
        Thread[] threads = new Thread[3];
        threads[0] = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                // 打印当前线程名称
                System.out.print(Thread.currentThread().getName());
                // 唤醒下一个线程
                LockSupport.unpark(threads[1]);
                // 当前线程阻塞
                LockSupport.park();
            }
        }, "A");
        threads[1] = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                // 先阻塞等待被唤醒
                LockSupport.park();
                System.out.print(Thread.currentThread().getName());
                // 唤醒下一个线程
                LockSupport.unpark(threads[2]);
            }
        }, "B");
        threads[2] = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                // 先阻塞等待被唤醒
                LockSupport.park();
                System.out.println(Thread.currentThread().getName());
                // 唤醒下一个线程
                LockSupport.unpark(threads[0]);
            }
        }, "C");
        threads[0].start();
        threads[1].start();
        threads[2].start();
    }

    @SuppressWarnings("DuplicatedCode")
    public static void threePrintUseReetraenLock() {
        ReentrantLock lock = new ReentrantLock();
        // 使用ReentrantLock的newCondition()方法创建三个Condition
        // 分别对应A、B、C三个线程
        Condition conditionA = lock.newCondition();
        Condition conditionB = lock.newCondition();
        Condition conditionC = lock.newCondition();

        // A线程
        new Thread(() -> {
            try {
                lock.lock();
                for (int i = 0; i < 10; i++) {
                    System.out.print(Thread.currentThread().getName());
                    // 叫醒B线程
                    conditionB.signal();
                    // 本线程阻塞
                    conditionA.await();
                }
                // 这里有个坑，要记得在循环之后调用signal()，否则线程可能会一直处于
                // wait状态，导致程序无法结束
                conditionB.signal();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                // 在finally代码块调用unlock方法
                lock.unlock();
            }
        }, "A").start();
        // B线程
        new Thread(() -> {
            try {
                lock.lock();
                for (int i = 0; i < 10; i++) {
                    System.out.print(Thread.currentThread().getName());
                    conditionC.signal();
                    conditionB.await();
                }
                conditionC.signal();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }, "B").start();
        // C线程
        new Thread(() -> {
            try {
                lock.lock();
                for (int i = 0; i < 10; i++) {
                    System.out.print(Thread.currentThread().getName());
                    conditionA.signal();
                    conditionC.await();
                }
                conditionA.signal();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }, "C").start();
    }

}
