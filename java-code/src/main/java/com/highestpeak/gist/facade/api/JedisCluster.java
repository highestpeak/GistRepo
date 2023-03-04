package com.highestpeak.gist.facade.api;

import javax.annotation.Nonnull;

import lombok.Data;

/**
 * redis 实例. 门面模式
 *
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-03
 */
@Data
public class JedisCluster {

    /**
     * WARNING: replace with real client
     */
    public static final JedisCluster MOCK_DEFAULT = new JedisCluster();

    /**
     * 执行接口
     */
    @Nonnull
    public RedisCommands get() {
        // WARNING: put you code which impl RedisCommands here
        //noinspection ConstantConditions
        return null;
    }

}
