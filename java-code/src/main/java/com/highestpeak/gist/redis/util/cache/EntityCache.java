package com.highestpeak.gist.redis.util.cache;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;
import com.google.protobuf.InvalidProtocolBufferException;
import com.highestpeak.gist.common.conf.Hconf;
import com.highestpeak.gist.common.conf.Hconfs;
import com.highestpeak.gist.common.help.BytesHelper;
import com.highestpeak.gist.facade.api.JedisCluster;
import com.highestpeak.gist.facade.api.RedisPipeline;
import com.highestpeak.gist.mess.util.BatchExecutor;
import com.highestpeak.gist.redis.util.cache.objects.StringObject;

import lombok.extern.slf4j.Slf4j;

/**
 * 实体/对象类型 Redis 缓存组件
 *
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-05
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@Slf4j
public class EntityCache<T extends StringObject> extends BaseRedisCache<T> {

    /**
     * 按照前缀限制 predicate 回表的 qps <br/>
     * 1. cache 中查询不存在一定回表 <br/>
     * 2. 如果没有配置限流，predicate 为 false 也可以回表 <br/>
     * 3. 如果配置了限流，cache 不存在的部分会消耗回表的 qps，predicate=false 也消耗回表 qps， 回表 qps 不足时，不能回表 <br/>
     */
    private static final Hconf<Map<String, RateLimiter>> PREFIX_CACHE_LOAD_PER_SECOND = Hconfs.ofDoubleMap(
            "highestpeak.prefix_cache_load_per_second", Collections.emptyMap())
            .mapper(stringDoubleMap -> {
                Map<String, RateLimiter> rateLimiterMap = Maps.newHashMapWithExpectedSize(stringDoubleMap.size());
                for (Map.Entry<String, Double> entry : stringDoubleMap.entrySet()) {
                    if (entry.getValue() > 0) {
                        rateLimiterMap.put(entry.getKey(), RateLimiter.create(entry.getValue()));
                    }
                }
                return rateLimiterMap;
            }).build();

    /**
     * 初始化
     *
     * @param redis  redis实例
     * @param prefix key的前缀
     */
    public EntityCache(JedisCluster redis, String prefix, Function<Collection<String>, Collection<T>> loader, int expire) {
        super(redis, prefix, loader, expire);
    }

    @Override
    public String key(T entity) {
        return entity.stringKey();
    }

    @Override
    public void cache(Collection<T> entities) {
        if (entities.isEmpty()) {
            return;
        }

        BatchExecutor.batchExecute(entities, BatchExecutor.DEFAULT_BATCH_SIZE, batch -> getRedis().pipeline(p -> {
            for (T entity : batch) {
                p.setex(
                        fullKey(entity.stringKey()),
                        getExpire(),
                        BytesHelper.toHex(entity.serialize())
                );
            }
        }));
    }

    /**
     * 获取 redis 中缓存的值,没有则使用 loader 进行 load
     *
     * @param supplier 大多数时候是传入'无参构造器',然后在 getCacheOnly 中的 data.deSerialize 实现数据填充
     */
    public Map<String, T> get(Collection<String> keys, Supplier<T> supplier) {
        return get(keys, s -> supplier.get(), t -> true);
    }

    /**
     * 获取 redis 中缓存的值,没有则使用 loader 进行 load,使用 predicate 执行元素过滤
     */
    public Map<String, T> get(Collection<String> keys, Function<String, T> func, Predicate<T> predicate) {
        Map<String, T> values = getCacheOnly(keys, func, predicate);
        Collection<T> loaded = loadAndCache(
                keys.stream().filter(k -> !values.containsKey(k)).collect(toSet())
        );
        for (T t : loaded) {
            values.put(t.stringKey(), t);
        }
        return values;
    }

    /**
     * 获取 redis 中缓存的值,没有则略过
     */
    public Map<String, T> getCacheOnly(Collection<String> keys, Function<String, T> func, Predicate<T> predicate) {
        RateLimiter rateLimiter = PREFIX_CACHE_LOAD_PER_SECOND.get().get(getPrefix());
        Map<String, String> cacheExists = pipelineFetch(keys, RedisPipeline::get);
        if (rateLimiter != null && cacheExists.size() < keys.size()) {
            // cache 不存在回表
            rateLimiter.tryAcquire(keys.size() - cacheExists.size());
        }

        return cacheExists.entrySet()
                .stream()
                .filter(entry -> {
                    boolean valid = entry != null && entry.getValue() != null;
                    if (!valid && rateLimiter != null) { // value=false 回表，消耗 qps
                        rateLimiter.tryAcquire();
                    }
                    return valid;
                })
                .map(entry -> {
                    T data = func.apply(entry.getKey()); // 使用 function 可以减少 redis 中冗余存储额外的 key
                    try {
                        data.deSerialize(BytesHelper.fromHex(entry.getValue()));
                    } catch (InvalidProtocolBufferException e) {
                        log.error("parse protobuf error, key {}", fullKey(entry.getKey()), e);
                    }
                    return data;
                })
                .filter(t -> { // 判断缓存中的数据是否降级可用
                    boolean valid = predicate.test(t);
                    if (valid) {
                        return true;
                    }
                    // 缓存过期时，如果配置 rateLimiter && 消耗 qps 失败时，重置 valid=true, 避免回表
                    boolean isBackTableLimit = rateLimiter != null && !rateLimiter.tryAcquire();
                    //noinspection StatementWithEmptyBody
                    if (isBackTableLimit) {
                        // WARNING: log you code here
                    }
                    return isBackTableLimit;
                })
                .collect(toMap(StringObject::stringKey, Function.identity()));
    }

}
