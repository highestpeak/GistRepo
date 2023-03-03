package com.highestpeak.gist.redis.util;

import java.time.Duration;

import org.junit.BeforeClass;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-03
 */
@SuppressWarnings("unused")
@Slf4j
class RedisKeyExpireCheckerTest {

    public static final int SECONDS = 3;

    private static RedisKeyExpireChecker autoRetryChecker;

    @BeforeClass
    public static void beforeClass() {
        autoRetryChecker = RedisKeyExpireChecker.newBuilder()
                .setUniqName("redis-key-expire-checker-test")
                .setShard(12)
                .setCheckIntervalSeconds(2)
                .setEnableAutoRetry(true)
                .setAutoRetryDuration(Duration.ofMinutes(5))
                .setConsumer(RedisKeyExpireCheckerTest::timeToBizExec)
                .build();
    }

    private static void timeToBizExec(String checkId) {
        log.info("timeToBizExec checkId: {}. Have a nice day.", checkId);
        autoRetryChecker.deleteKey(checkId);
    }

    @Test
    public void checkExpire() {
        RedisKeyExpireChecker redisKeyExpireChecker = RedisKeyExpireChecker.newBuilder()
                .setUniqName("redis-key-expire-checker-test-2")
                .setShard(12)
                .setConsumer(this::timeToExec)
                .build();

        redisKeyExpireChecker.checkWhenFixDelay("aaa", Duration.ofSeconds(SECONDS));
        redisKeyExpireChecker.checkWhenFixDelay("bbb", Duration.ofSeconds(-SECONDS));
        redisKeyExpireChecker.checkExpireManual();
        redisKeyExpireChecker.checkExpireManual();
        redisKeyExpireChecker.checkExpireManual();
    }

    private void timeToExec(String checkId) {
        log.info("timeToExec checkId: {}. Have a nice day.", checkId);
    }

}