package com.highestpeak.gist.common.conf;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-03
 */
public interface IHconfFactory {

    HconfBuilder<Integer> ofInteger(String key, int defaultValue);

    HconfBuilder<Long> ofLong(String key, long defaultValue);

    HconfBuilder<Double> ofDouble(String key, double defaultValue);

    HconfBuilder<Boolean> ofBoolean(String key, boolean defaultValue);

    HconfBuilder<String> ofString(String key, String defaultValue);

    HconfBuilder<ByteString> ofBytes(String key, ByteString defaultValue);

    <T extends GeneratedMessageV3> HconfBuilder<T> ofProto(String key, T defaultValue, Class<T> tClass);

    <T> HconfBuilder<T> ofJson(String key, T defaultValue, Class<T> tClass);

    <V> HconfBuilder<List<V>> ofJsonList(String key, List<V> defaultValue, Class<V> itemType);

    <V> HconfBuilder<Set<V>> ofJsonSet(String key, Set<V> defaultValue, Class<V> itemType);

    <T extends Map<K, V>, K, V> HconfBuilder<T> ofJsonMap(String key, T defaultValue, Class<K> keyType, Class<V> itemType);

    <T extends Map<K, V>, K, V> HconfBuilder<T> ofJsonMap(String key, T defaultValue, Class<? extends Map> mapType, Class<K> keyType, Class<V> itemType);

    <T extends Map<K, Set<V>>, K, V> HconfBuilder<T> ofSetMap(String key, T defaultValue, Class<K> keyType, Class<V> subItemType);

    <T extends Map<K, List<V>>, K, V> HconfBuilder<T> ofListMap(String key, T defaultValue, Class<K> keyType, Class<V> subItemType);

    <T extends Map<K, Map<K1, V1>>, K, K1, V1> HconfBuilder<T> ofMapMap(String key, T defaultValue, Class<K> keyType, Class<K1> subKeyClass, Class<V1> subItemType);

    // ============ list constructors ============ //

    HconfBuilder<List<Integer>> ofIntegerList(String key, List<Integer> defaultValue);

    HconfBuilder<List<Long>> ofLongList(String key, List<Long> defaultValue);

    HconfBuilder<List<Double>> ofDoubleList(String key, List<Double> defaultValue);

    HconfBuilder<List<String>> ofStringList(String key, List<String> defaultValue);

    // ============ set constructors ============ //

    HconfBuilder<Set<Integer>> ofIntegerSet(String key, Set<Integer> defaultValue);

    HconfBuilder<Set<Long>> ofLongSet(String key, Set<Long> defaultValue);

    HconfBuilder<Set<Double>> ofDoubleSet(String key, Set<Double> defaultValue);

    HconfBuilder<Set<String>> ofStringSet(String key, Set<String> defaultValue);

    // ============= map constructors ============== //

    HconfBuilder<Map<String, Integer>> ofIntegerMap(String key, Map<String, Integer> defaultValue);

    <K> HconfBuilder<Map<K, Integer>> ofIntegerMap(String key, Map<K, Integer> defaultValue, MapKeyParser<K> keyParser);

    HconfBuilder<Map<String, Long>> ofLongMap(String key, Map<String, Long> defaultValue);

    <K> HconfBuilder<Map<K, Long>> ofLongMap(String key, Map<K, Long> defaultValue, MapKeyParser<K> keyParser);

    HconfBuilder<Map<String, Double>> ofDoubleMap(String key, Map<String, Double> defaultValue);

    <K> HconfBuilder<Map<K, Double>> ofDoubleMap(String key, Map<K, Double> defaultValue, MapKeyParser<K> keyParser);

    HconfBuilder<Map<String, String>> ofStringMap(String key, Map<String, String> defaultValue);

    <K> HconfBuilder<Map<K, String>> ofStringMap(String key, Map<K, String> defaultValue, MapKeyParser<K> keyParser);

    HconfBuilder<Map<String, Boolean>> ofBooleanMap(String key, Map<String, Boolean> defaultValue);

    <K> HconfBuilder<Map<K, Boolean>> ofBooleanMap(String key, Map<K, Boolean> defaultValue, MapKeyParser<K> keyParser);

    <K, V> HconfBuilder<Map<K, V>> ofMap(String key, Map<K, V> defaultValue, MapKeyParser<K> keyParser, ValueParser<V> valueParser);

    HconfBuilder<Value> ofRawValue(String key);
}

