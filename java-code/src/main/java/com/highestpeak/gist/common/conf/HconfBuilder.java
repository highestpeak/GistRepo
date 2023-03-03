package com.highestpeak.gist.common.conf;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;


/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-03
 */
public interface HconfBuilder<T> {

    /**
     * mapper 的作用：
     * 1. 类型转换
     * 2. 合法性校验
     * <p>
     * mapper 实现的原则：<br/>
     * 1. Fail Fast，仔细校验格式，如果不合法直接抛出异常，框架层会自动返回上一个正确的值或者默认值 <br/>
     * 2. 切忌把 mapper 当作变更回调向外产生副作用，如果 mapper 抛出异常，很容易产生状态不一致 <br/>
     * <p>
     * 其他：
     * 如果出现异常 hconf 就会使用上一次的合法值或者默认值
     */
    @CheckReturnValue
    <T2> HconfBuilder<T2> mapper(ConfigMapper<T, T2> mapper);

    default Hconf<T> build() {
        return build(null);
    }

    default Hconf<T> build(@Nullable Consumer<T> cleanup) {
        return buildVersioned(cleanup);
    }

    default VersionedHconf<T> buildVersioned() {
        return buildVersioned(null);
    }

    VersionedHconf<T> buildVersioned(@Nullable Consumer<T> cleanup);

    /**
     * Must call after all mappers set.
     * First init change is not notified, users should directly use .get() for initialization.
     */
    HconfBuilder<T> onDataChange(HconfDataChangeListener<T> listener, ThreadPoolExecutor executor);

    /**
     * ThreadPoolExecutor -> ExecutorService, 避免重载二进制不兼容
     */
    HconfBuilder<T> onDataChange(HconfDataChangeListener<T> listener, ExecutorService executor);

}
