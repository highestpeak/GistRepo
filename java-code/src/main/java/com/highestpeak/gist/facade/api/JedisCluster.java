package com.highestpeak.gist.facade.api;

import static java.util.Collections.singleton;
import static java.util.function.Function.identity;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;

import lombok.Data;

/**
 * redis 实例. 门面模式
 *
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-03
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
@Data
public class JedisCluster {

    /**
     * WARNING: replace with real client
     */
    public static final JedisCluster MOCK_DEFAULT = new JedisCluster();

    private static final Object EMPTY_KEY = new Object();

    /**
     * 执行接口
     */
    @Nonnull
    public RedisCommands get() {
        // WARNING: put you code which impl RedisCommands here
        //noinspection ConstantConditions
        return null;
    }

    public void pipeline(Consumer<RedisPipeline> function) {
        pipeline(p -> {
            function.accept(p);
            return null;
        });
    }

    public <V> V pipeline(Function<RedisPipeline, PipelineResponse<V>> function) {
        return pipeline(singleton(EMPTY_KEY), (p, k) -> function.apply(p)).get(EMPTY_KEY);
    }

    public <K, V> Map<K, V> pipeline(Iterable<K> keys, BiFunction<RedisPipeline, K, PipelineResponse<V>> function) {
        return pipeline(keys, function, true);
    }

    public <K, V> Map<K, V> pipeline(Iterable<K> keys, BiFunction<RedisPipeline, K, PipelineResponse<V>> function, boolean includeNullValue) {
        return pipeline(keys, function, identity(), includeNullValue);
    }

    public <K, V, T> Map<K, T> pipeline(Iterable<K> keys, BiFunction<RedisPipeline, K, PipelineResponse<V>> function, Function<V, T> decoder) {
        return pipeline(keys, function, decoder, true);
    }

    /**
     * 底层采用String类型的pipeline操作{@link RedisPipeline}，将操作返回的V类型的结果通过decoder转码成T类型
     *
     * @param keys             pipeline操作的一系列Keys
     * @param includeNullValue 是否允许返回值V为null
     * @param <K>              key的类型
     * @param <V>              返回值的类型
     * @param <T>              （将V）转码的目标类型
     * @return map, key为Redis key,value为对应的转码后的返回值
     */
    public <K, V, T> Map<K, T> pipeline(
            Iterable<K> keys, BiFunction<RedisPipeline, K, PipelineResponse<V>> function,
            Function<V, T> decoder, boolean includeNullValue) {
        // WARNING: put you code which impl pipeline here
        return null;
    }


}
