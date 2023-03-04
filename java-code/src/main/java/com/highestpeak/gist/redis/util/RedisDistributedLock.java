package com.highestpeak.gist.redis.util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.highestpeak.gist.facade.api.JedisCluster;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * redis 分布式锁. 用于并发量比较小的并行业务控制场景，支持获取多个锁
 * todo WARNING 释放锁的操作采用 get + del 非原子操作 写一个版本使用 redis 的 script 命令来进行锁释放(可能有时候现实情况不允许使用 script 的方式,所以默认使用 get + del 先实现)
 *
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-04
 */
@SuppressWarnings("unused")
@Slf4j
public class RedisDistributedLock {

    private static final String REDIS_LOCK_PRE = "lock:nx:ex:";

    /**
     * 分布式锁依赖的 redis 集群
     */
    private final JedisCluster redisClient;
    /**
     * 获取锁失败时，最大重试次数
     */
    private final int maxRetry;
    /**
     * 获取锁失败时，重试时的间隔时间
     */
    private final long retryIntervalMillis;
    /**
     * 获取锁成功后，锁自动过期的时间
     */
    private final long lockHoldTimeoutMillis;

    /**
     * 分布式锁的构造方法
     *
     * @param unit retryInterval 和 lockHoldTimeout 参数的单位
     */
    public RedisDistributedLock(JedisCluster redisClient, int maxRetry, long retryInterval, long lockHoldTimeout, TimeUnit unit) {
        this.redisClient = redisClient;
        this.maxRetry = Math.max(1, maxRetry);
        this.retryIntervalMillis = unit.toMillis(retryInterval);
        this.lockHoldTimeoutMillis = unit.toMillis(lockHoldTimeout);
    }

    /**
     * 支持同时对多个 key 加锁，会对加锁 key 排序
     */
    public <T> T callWithLock(RedisLockTask<T> task) {
        Preconditions.checkArgument(task != null && CollectionUtils.isNotEmpty(task.getLockKeys()));
        List<String> lockKeys = getSortedLockKeys(task.getLockKeys());
        String lockValue = UUID.randomUUID().toString();

        int execTimes = 0;
        while (execTimes < maxRetry) {
            execTimes++;
            LockResponse lockResponse = lock(lockKeys, lockValue, lockHoldTimeoutMillis);
            if (lockResponse.isLocked()) {
                try {
                    if (task.distinctMills() > 0 && needFilter(lockKeys, task.distinctMills())) {
                        log.info("needFiler keys: {}", lockKeys);
                        return null;
                    }
                    return task.callInLock();
                } finally {
                    unlock(lockKeys, lockValue);
                }
            } else {
                unlock(lockResponse.getNeedUnlockKeys(), lockValue);
                if (execTimes >= maxRetry) { // 是否超过最大重试次数
                    break;
                }
                try {
                    Thread.sleep(retryIntervalMillis);
                } catch (InterruptedException e) {
                    // 等待时响应中断退出，并设置中断状态
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new RedisLockTimeoutException("lockKeys:" + lockKeys + ", wait timeout");
    }

    private List<String> getSortedLockKeys(List<String> lockKeys) {
        return lockKeys.stream()
                .distinct()
                // 注意这个 sort
                .sorted()
                .map(key -> REDIS_LOCK_PRE + key)
                .collect(Collectors.toList());
    }

    private boolean needFilter(List<String> lockKeys, long expireMills) {
        String filerKey = "filter:" + Joiner.on(":").join(lockKeys);
        String flag = redisClient.get().set(filerKey, String.valueOf(System.currentTimeMillis()), "NX", "PX", expireMills);
        return StringUtils.isBlank(flag); // 已经存在，则为空
    }

    private LockResponse lock(List<String> lockKeys, String lockValue, long expireMillis) {
        List<String> needUnlockKeys = new ArrayList<>(lockKeys.size());
        for (String key : lockKeys) {
            String flag = redisClient.get().set(key, lockValue, "NX", "PX", expireMillis);
            boolean currentLock = StringUtils.isNotBlank(flag);
            if (currentLock) {
                needUnlockKeys.add(key);
            } else {
                log.info("lock conflict, key: {}, flag: {}", key, flag);
                break;
            }
        }
        LockResponse lockResponse = new LockResponse();
        lockResponse.setLocked(lockKeys.size() == needUnlockKeys.size());
        lockResponse.setNeedUnlockKeys(needUnlockKeys);
        return lockResponse;
    }

    private void unlock(List<String> lockKeys, String lockValue) {
        if (CollectionUtils.isEmpty(lockKeys)) {
            return;
        }
        for (String key : lockKeys) {
            String currentLockValue = redisClient.get().get(key);
            if (lockValue.equals(currentLockValue)) {
                redisClient.get().del(key);
            }
        }
    }

    /**
     * 获取锁定的结果状态
     */
    @Data
    private static class LockResponse {
        /**
         * 是否加锁成功
         */
        private boolean locked;
        /**
         * 需要解锁的列表，加锁失败时，可以持有部分锁，也需要解锁
         */
        private List<String> needUnlockKeys;
    }


    public interface RedisLockTask<T> {
        /**
         * 业务执行需要的多个锁
         */
        List<String> getLockKeys();

        /**
         * 需要在分布式锁中执行的任务
         */
        T callInLock();

        /**
         * 任务执行时的防抖去重逻辑，避免任务在短时间内被重复执行
         * 当返回值 > 0 时，间隔时间小于 distinctMills 时，会跳过执行，返回 null
         * 当返回 <= 0 时，直接执行
         *
         * @return 设置的去重间隔，默认不进行去重
         */
        default long distinctMills() {
            return 0L;
        }
    }

    public static class RedisLockTimeoutException extends RuntimeException {

        public RedisLockTimeoutException(String message) {
            super(message);
        }

        public RedisLockTimeoutException(String message, Throwable cause) {
            super(message, cause);
        }

        public RedisLockTimeoutException(Throwable cause) {
            super(cause);
        }

        public RedisLockTimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

}
