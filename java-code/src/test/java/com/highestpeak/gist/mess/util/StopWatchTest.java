package com.highestpeak.gist.mess.util;

import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-08
 */
@Slf4j
public class StopWatchTest {

    /**
     * 第一种使用情况: 同一个线程上下文中打印耗时信息 <br/>
     * 注意: 实际使用过程种，里面全都拿不到外层的 StopWatch 实例
     */
    @Test
    public void case1TestContextStopWatch() {
        StopWatch.SessionWatcher watcher = StopWatch.createSessionWatcher(); // todo 创建了之后就记录了 start
        method("method-1", () -> {
            method("method-1-1", () -> {
            });
            method("method-1-2", () -> {
                method("method-1-2-1", () -> {
                });
            });
            method("method-1-3", () -> {
            });
        });
        method("method-2", () -> {
            method("method-2-1", () -> {
                method("method-2-1-1", () -> {
                });
                method("method-2-1-2", () -> {
                });
                method("method-2-1-3", () -> {
                });
            });
            method("method-2-2", () -> {
            });
        });
        method("method-3", () -> {
        });
    }

    private void method(String methodName, Runnable innerMethodRun) {
        log.info("method run. methodName: {}", methodName);
        innerMethodRun.run();
    }

}