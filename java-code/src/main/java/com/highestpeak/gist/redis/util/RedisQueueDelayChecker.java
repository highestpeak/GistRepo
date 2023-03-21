package com.highestpeak.gist.redis.util;

import java.time.Duration;
import java.util.function.Supplier;

import com.highestpeak.gist.common.help.RedisTuple;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * RedisKeyExpireChecker 分数是到期时间，RedisQueueDelayChecker 实现是分数是写入时间，到期后按照需要，从 Hconf 读出配置的过期时间长度，然后从队列消费需要消费的数据 <br/>
 * 该方式的优点是可以根据 consumerTimeMillsBeforeCurrSupplier 动态设置时间
 *
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-21
 */
@SuppressWarnings("unused")
@Slf4j
public class RedisQueueDelayChecker extends RedisKeyExpireChecker {

    /**
     * 当使用 peekCheckScoreNotCurrTime 时, peekFirst 消费多长时间以前的任务
     */
    private final Supplier<Long> consumerTimeMillsBeforeCurrSupplier;

    public RedisQueueDelayChecker(Builder builder, @NonNull Supplier<Long> consumerTimeMillsBeforeCurrSupplier) {
        super(builder);
        this.consumerTimeMillsBeforeCurrSupplier = consumerTimeMillsBeforeCurrSupplier;
    }

    public void checkWithScoreWhenIdleDelay(String checkId) {
        checkWhenIdleDelay(checkId, Duration.ZERO);
    }

    /**
     * only for peekCheckScoreNotCurrTime is ok
     */
    public void checkScoreWhenFixDelay(String checkId) {
        checkWhenFixDelay(checkId, Duration.ZERO);
    }

    @Override
    protected boolean checkIfFirstDataOk(RedisTuple firstData) {
        // 执行时机: firstData.score + consumerTimeMillsBeforeCurrSupplier <= 当前时间
        long firstEleExpireWait = firstData.getScore().longValue() + consumerTimeMillsBeforeCurrSupplier.get();
        return firstEleExpireWait <= System.currentTimeMillis();
    }

    @Override
    protected double autoRetrySource(FirstData firstData) {
        return System.currentTimeMillis() - consumerTimeMillsBeforeCurrSupplier.get() + getAutoRetryDuration().toMillis();
    }

}
