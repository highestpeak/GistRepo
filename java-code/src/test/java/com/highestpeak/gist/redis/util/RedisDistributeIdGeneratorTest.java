package com.highestpeak.gist.redis.util;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

import com.highestpeak.gist.facade.api.JedisCluster;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-04
 */
public class RedisDistributeIdGeneratorTest {

    @Test
    public void testGenId() {
        RedisDistributeIdGenerator.RedisMachineIdProvider redisMachineIdProvider = new RedisDistributeIdGenerator.RedisMachineIdProvider(
                "highestpeak_test", JedisCluster.MOCK_DEFAULT
        );
        RedisDistributeIdGenerator idGenerator = new RedisDistributeIdGenerator(redisMachineIdProvider);

        AtomicBoolean wrong = new AtomicBoolean(false);
        Set<Long> idSet = new ConcurrentSkipListSet<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 1000; j++) {
                executor.execute(() -> {
                    Long uid = idGenerator.generateId();
                    if (!idSet.add(uid)) {
                        System.err.println("duplicate id:" + uid);
                        wrong.set(true);
                    }
                });
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
        }
        executor.shutdown();
        try {
            //noinspection ResultOfMethodCallIgnored
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
        }
        System.out.println("idSet = " + idSet);
        Assert.assertFalse(wrong.get());
    }


}