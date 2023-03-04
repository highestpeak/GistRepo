package com.highestpeak.gist.redis.util;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.highestpeak.gist.facade.api.JedisCluster;

import lombok.extern.slf4j.Slf4j;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-04
 */
@Slf4j
public class RedisDistributedLockTest {

    private final RedisDistributedLock redisLock = new RedisDistributedLock(
            JedisCluster.MOCK_DEFAULT, 7, 1, 10, TimeUnit.SECONDS
    );

    @Test
    public void testLock() throws InterruptedException {
        for (int i = 0; i < 4; i++) {
            invokeRedisLock(i);
        }
        TimeUnit.SECONDS.sleep(2);
        for (int i = 5; i < 7; i++) {
            invokeRedisLock(i);
        }
    }

    private void invokeRedisLock(int i) {
        redisLock.callWithLock(new RedisDistributedLock.RedisLockTask<Void>() {
            @Override
            public List<String> getLockKeys() {
                return Collections.singletonList("a");
            }

            @Override
            public Void callInLock() {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException ignore) {
                }
                log.info("task = {}", i);
                return null;
            }

            @Override
            public long distinctMills() {
                return TimeUnit.SECONDS.toMillis(2);
            }
        });
    }

}