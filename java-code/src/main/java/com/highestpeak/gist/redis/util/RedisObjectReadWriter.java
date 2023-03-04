package com.highestpeak.gist.redis.util;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.highestpeak.gist.facade.api.JedisCluster;
import com.highestpeak.gist.mess.util.JsonUtil;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 读写 redis 缓存对象 的辅助类
 *
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-03
 */
@SuppressWarnings("unused")
@Slf4j
@AllArgsConstructor
public class RedisObjectReadWriter {

    /**
     * 用来拼接例如前缀作为key
     */
    private final Function<String, String> keyMapper;

    private final JedisCluster redisCluster;

    private String innerKeyToFullKey(String innerKey) {
        return keyMapper.apply(innerKey);
    }

    public void bindObject(String innerKey, Object obj, Duration duration) {
        redisCluster.get().setex(
                innerKeyToFullKey(innerKey),
                (int) (duration.toMillis() / 1000),
                JsonUtil.toJson(obj)
        );
    }

    public <T> Optional<T> readObject(String innerKey, Class<T> clazz) {
        return readObject(
                innerKeyToFullKey(innerKey),
                TypeFactory.defaultInstance().constructType(clazz)
        );
    }

    public <T> Optional<T> readObject(String innerKey, TypeReference<T> typeReference) {
        return readObject(
                innerKeyToFullKey(innerKey),
                TypeFactory.defaultInstance().constructType(typeReference)
        );
    }

    public <T> Optional<T> readObject(String innerKey, JavaType javaType) {
        return Optional.ofNullable(redisCluster.get().get(innerKeyToFullKey(innerKey)))
                .filter(StringUtils::isNotBlank)
                .map(attachVal -> JsonUtil.fromJson(attachVal, javaType));
    }


}
