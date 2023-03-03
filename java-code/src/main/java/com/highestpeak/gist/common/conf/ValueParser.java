package com.highestpeak.gist.common.conf;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-03
 */
public interface ValueParser<T> {

    T parse(Value value) throws Exception;
}

