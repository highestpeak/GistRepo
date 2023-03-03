package com.highestpeak.gist.common.conf;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-03
 */
public interface VersionedHconf<T> extends Hconf<T> {

    /**
     * 获取配置和对应的版本号
     */
    VersionedData<T> getVersioned();
}

