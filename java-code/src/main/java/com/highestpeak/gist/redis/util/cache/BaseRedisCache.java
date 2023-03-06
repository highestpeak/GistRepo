package com.highestpeak.gist.redis.util.cache;

import static java.util.Collections.singleton;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.highestpeak.gist.common.help.RedisTuple;
import com.highestpeak.gist.facade.api.JedisCluster;
import com.highestpeak.gist.facade.api.PipelineResponse;
import com.highestpeak.gist.facade.api.RedisPipeline;
import com.highestpeak.gist.mess.util.BatchExecutor;

import lombok.Getter;

/**
 * Redis缓存组件基础类
 *
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-05
 */
@SuppressWarnings("unused")
public abstract class BaseRedisCache<T> {

    @Getter
    private final JedisCluster redis;
    @Getter
    private final int expire;
    @Getter
    private final String prefix;
    private final Function<Collection<String>, Collection<T>> loader;

    /**
     * 初始化
     *
     * @param redis  redis 实例
     * @param prefix key 的前缀
     * @param loader 加载数据的 loader: loader 加载完数据后,本工具类负责缓存到 redis
     */
    public BaseRedisCache(JedisCluster redis, String prefix, Function<Collection<String>, Collection<T>> loader, int expire) {
        this.redis = redis;
        this.prefix = prefix;
        this.loader = loader;
        this.expire = expire;
    }

    public abstract String key(T entity);

    public abstract void cache(Collection<T> entities);

    // key mapper

    private Set<String> entitiesToKey(Collection<T> entities) {
        return entities.stream().map(this::key).collect(Collectors.toSet());
    }

    public String fullKey(String key, String suffix) {
        return prefix + key + ":" + suffix;
    }

    public String fullKey(String realKey) {
        return prefix + realKey;
    }

    public String realKey(String fullKey) {
        return fullKey.substring(prefix.length());
    }

    // invalidateKey

    public void invalidate(Collection<T> entities) {
        invalidateKeys(entitiesToKey(entities));
    }

    public void invalidateKey(String key) {
        invalidateKeys(singleton(key));
    }

    public void invalidateKeys(Collection<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        invalidateSpecialVersionKeys(keys, this::fullKey);
    }

    public void invalidateSpecialVersionEntities(Collection<T> entities, Function<String, String> fullKeySupplier) {
        invalidateSpecialVersionKeys(entities.stream().map(this::key).collect(Collectors.toSet()), fullKeySupplier);
    }

    public void invalidateSpecialVersionKeys(Collection<String> keys, Function<String, String> fullKeySupplier) {
        BatchExecutor.batchExecute(keys, BatchExecutor.DEFAULT_BATCH_SIZE, batch -> redis.pipeline(p -> {
            for (String key : batch) {
                p.del(fullKeySupplier.apply(key));
            }
        }));
    }

    // todo operate

    public boolean exists(String key) {
        return redis.get().exists(fullKey(key));
    }

    public void zAdd(String key, String member, Double score) {
        Map<String, Double> item = new HashMap<>();
        item.put(member, score);
        redis.get().zadd(fullKey(key), item);
    }

    public void sAdd(String key, String... members) {
        redis.get().sadd(key, members);
    }

    public void sRem(String key, String... members) {
        redis.get().srem(key, members);
    }

    public void zAdd(String key, Map<String, Double> scoreMembers) {
        redis.get().zadd(fullKey(key), scoreMembers);
    }

    public void zRem(String key, Collection<String> members) {
        redis.get().zrem(fullKey(key), members.toArray(new String[]{}));
    }

    public Set<RedisTuple> zRevRangeByScoreWithScores(String key, double max, double min, int offset, int limit) {
        return redis.get().zrevrangeByScoreWithScores(fullKey(key), max, min, offset, limit);
    }

    public Set<RedisTuple> zRangeByScoreWithScores(String key, double max, double min, int offset, int limit) {
        return redis.get().zrangeByScoreWithScores(fullKey(key), max, min, offset, limit);
    }

    // todo batch load & batch fetch

    public Collection<T> loadAndCache(Set<String> keys) {
        if (keys.isEmpty()) {
            return Collections.emptySet();
        }
        Set<T> loaded = Sets.newHashSet();
        BatchExecutor.batchExecute(keys, BatchExecutor.DEFAULT_BATCH_SIZE,
                batch -> loaded.addAll(loader.apply(batch))
        );
        if (!loaded.isEmpty()) {
            cache(loaded);
        }
        return loaded;
    }

    public <R> Map<String, R> pipelineFetch(Collection<String> realKeys, BiFunction<RedisPipeline, String, PipelineResponse<R>> function) {
        Map<String, R> collector = Maps.newHashMap();
        BatchExecutor.batchExecute(realKeys, BatchExecutor.DEFAULT_BATCH_SIZE, realKeyBatch ->
                redis.pipeline(
                        realKeyBatch.stream().map(this::fullKey).collect(Collectors.toSet()),
                        function
                ).forEach((fullKey, value) ->
                        collector.put(realKey(fullKey), value)
                )
        );
        return collector;
    }

}
