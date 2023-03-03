package com.highestpeak.gist.common.conf;

import com.github.phantomthief.util.ThrowableFunction;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-03
 */
public interface ConfigMapper<T, R> extends ThrowableFunction<T, R, Exception> {

}