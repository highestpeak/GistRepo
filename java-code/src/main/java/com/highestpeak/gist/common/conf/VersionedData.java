package com.highestpeak.gist.common.conf;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-03
 */
@Data
@AllArgsConstructor
public class VersionedData<T> {

    /**
     * 配置版本号. 对应hconf历史记录中的版本号
     */
    private final long version;
    private final T data;

}
