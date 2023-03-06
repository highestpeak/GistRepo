package com.highestpeak.gist.facade.api;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-05
 */
public abstract class Builder<T> {
    public abstract T build(Object data);
}
