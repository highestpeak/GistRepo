package com.highestpeak.gist.common.conf;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-03
 */
public interface MapKeyParser<T> {

    T parse(String key) throws Exception;
}

