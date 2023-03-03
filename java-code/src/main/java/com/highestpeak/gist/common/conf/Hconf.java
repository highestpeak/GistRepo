package com.highestpeak.gist.common.conf;

import java.util.function.Supplier;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-03
 */
public interface Hconf<T> extends Supplier<T> {

    @Override
    T get();

    default void tryWarmup() {
        // do nothing
    }

}
