package com.highestpeak.gist.common;

import javax.annotation.Nonnull;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-03
 */
public interface WarmupAble {

    // todo

    /**
     * 参考实现方法：
     * 在本方法中先调用
     * {@link WarmUpHelper#markWarmup(WarmupAble)}（参数传入this）
     * 然后执行初始化资源
     * <p>
     * 在初始化资源中，应该调用{@link WarmUpHelper#markInit(WarmupAble)}
     */
    void tryWarmup();

    /**
     * 提供自适应warmup资源能力
     *
     * @param type 资源名称，不同组件尽量保证唯一
     */
    default void tryWarmup(@Nonnull String type) {

    }

}
