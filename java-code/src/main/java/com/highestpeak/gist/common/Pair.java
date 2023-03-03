package com.highestpeak.gist.common;

import lombok.Data;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-03
 */
@Data
public class Pair<K, V> {

    private final K key;
    private final V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }
}
