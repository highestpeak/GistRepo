package com.highestpeak.gist.common;

import lombok.Data;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-03
 */
@SuppressWarnings("unused")
@Data
public class Pair<K, V> {

    private final K key;
    private final V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public static <K, V> Pair<K, V> of(K key, V value) {
        return new Pair<>(key, value);
    }

    public K left() {
        return key;
    }

    public K first() {
        return key;
    }

    public V right() {
        return value;
    }

    public K second() {
        return key;
    }

}
