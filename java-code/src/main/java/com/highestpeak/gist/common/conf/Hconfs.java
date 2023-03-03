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
@SuppressWarnings("unused")
public class Hconfs {

    private static final IHconfFactory FACTORY;

    static {
        try {
            Class<?> factoryClass = Class.forName("com.highestpeak.hconf.client.HconfFactory");
            FACTORY = (IHconfFactory) factoryClass.getMethod("getInstance").invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("hconf init failed, implementation class not found.", e);
        }
    }

    private Hconfs() {
        // util class
    }

    /**
     * 配置类型: int32
     */
    public static HconfBuilder<Integer> ofInteger(String key, int defaultValue) {
        return FACTORY.ofInteger(key, defaultValue);
    }

    /**
     * 配置类型: int64
     */
    public static HconfBuilder<Long> ofLong(String key, long defaultValue) {
        return FACTORY.ofLong(key, defaultValue);
    }

    /**
     * 配置类型: double
     */
    public static HconfBuilder<Double> ofDouble(String key, double defaultValue) {
        return FACTORY.ofDouble(key, defaultValue);
    }

    /**
     * 配置类型: bool
     */
    public static HconfBuilder<Boolean> ofBoolean(String key, boolean defaultValue) {
        return FACTORY.ofBoolean(key, defaultValue);
    }

    /**
     * 配置类型: string
     */
    public static HconfBuilder<String> ofString(String key, String defaultValue) {
        return FACTORY.ofString(key, defaultValue);
    }

    /**
     * 配置类型: binary
     */
    public static HconfBuilder<ByteString> ofBytes(String key, ByteString defaultValue) {
        return FACTORY.ofBytes(key, defaultValue);
    }

    public static <T extends GeneratedMessageV3> HconfBuilder<T> ofProto(String key, T defaultValue, Class<T> tClass) {
        return FACTORY.ofProto(key, defaultValue, tClass);
    }

    /**
     * 配置类型: json
     */
    public static <T> HconfBuilder<T> ofJson(String key, T defaultValue, Class<T> tClass) {
        return FACTORY.ofJson(key, defaultValue, tClass);
    }

    /**
     * 配置类型: json
     */
    public static <V> HconfBuilder<List<V>> ofJsonList(String key, List<V> defaultValue, Class<V> itemType) {
        return FACTORY.ofJsonList(key, defaultValue, itemType);
    }

    /**
     * 配置类型: json
     */
    public static <V> HconfBuilder<Set<V>> ofJsonSet(String key, Set<V> defaultValue, Class<V> itemType) {
        return FACTORY.ofJsonSet(key, defaultValue, itemType);
    }

    /**
     * 配置类型: json
     */
    public static <T extends Map<K, V>, K, V> HconfBuilder<T> ofJsonMap(String key, T defaultValue, Class<K> keyType, Class<V> itemType) {
        return FACTORY.ofJsonMap(key, defaultValue, keyType, itemType);
    }

    /**
     * 配置类型: json
     */
    public static <T extends Map<K, V>, K, V> HconfBuilder<T> ofJsonMap(
            String key, T defaultValue, Class<? extends Map> mapType, Class<K> keyType, Class<V> itemType) {
        return FACTORY.ofJsonMap(key, defaultValue, mapType, keyType, itemType);
    }

    /**
     * 配置类型: json
     */
    public static <T extends Map<K, Set<V>>, K, V> HconfBuilder<T> ofSetMap(String key, T defaultValue, Class<K> keyType, Class<V> subItemType) {
        return FACTORY.ofSetMap(key, defaultValue, keyType, subItemType);
    }

    /**
     * 配置类型: json
     */
    public static <T extends Map<K, List<V>>, K, V> HconfBuilder<T> ofListMap(String key, T defaultValue, Class<K> keyType, Class<V> subItemType) {
        return FACTORY.ofListMap(key, defaultValue, keyType, subItemType);
    }

    /**
     * 配置类型: json
     */
    public static <T extends Map<K, Map<K1, V1>>, K, K1, V1> HconfBuilder<T> ofMapMap(
            String key, T defaultValue, Class<K> keyType, Class<K1> subKeyClass, Class<V1> subItemType) {
        return FACTORY.ofMapMap(key, defaultValue, keyType, subKeyClass, subItemType);
    }

    // ============ list constructors ============ //

    /**
     * 配置类型: list_int32
     */
    public static HconfBuilder<List<Integer>> ofIntegerList(String key, List<Integer> defaultValue) {
        return FACTORY.ofIntegerList(key, defaultValue);
    }

    /**
     * 配置类型: list_int64
     */
    public static HconfBuilder<List<Long>> ofLongList(String key, List<Long> defaultValue) {
        return FACTORY.ofLongList(key, defaultValue);
    }

    /**
     * 配置类型: list_double
     */
    public static HconfBuilder<List<Double>> ofDoubleList(String key, List<Double> defaultValue) {
        return FACTORY.ofDoubleList(key, defaultValue);
    }

    /**
     * 配置类型: list_string
     */
    public static HconfBuilder<List<String>> ofStringList(String key, List<String> defaultValue) {
        return FACTORY.ofStringList(key, defaultValue);
    }

    // ============ set constructors ============ //

    /**
     * 配置类型: set_int32
     */
    public static HconfBuilder<Set<Integer>> ofIntegerSet(String key, Set<Integer> defaultValue) {
        return FACTORY.ofIntegerSet(key, defaultValue);
    }

    /**
     * 配置类型: set_int64
     */
    public static HconfBuilder<Set<Long>> ofLongSet(String key, Set<Long> defaultValue) {
        return FACTORY.ofLongSet(key, defaultValue);
    }

    /**
     * 配置类型: set_double
     */
    public static HconfBuilder<Set<Double>> ofDoubleSet(String key, Set<Double> defaultValue) {
        return FACTORY.ofDoubleSet(key, defaultValue);
    }

    /**
     * 配置类型: set_string
     */
    public static HconfBuilder<Set<String>> ofStringSet(String key, Set<String> defaultValue) {
        return FACTORY.ofStringSet(key, defaultValue);
    }

    // ============= map constructors ============== //

    /**
     * 配置类型: map_string_int32
     */
    public static HconfBuilder<Map<String, Integer>> ofIntegerMap(String key, Map<String, Integer> defaultValue) {
        return FACTORY.ofIntegerMap(key, defaultValue);
    }

    /**
     * 配置类型: map_string_int32
     */
    public static <K> HconfBuilder<Map<K, Integer>> ofIntegerMap(String key, Map<K, Integer> defaultValue, MapKeyParser<K> keyParser) {
        return FACTORY.ofIntegerMap(key, defaultValue, keyParser);
    }

    /**
     * 配置类型: map_string_int64
     */
    public static HconfBuilder<Map<String, Long>> ofLongMap(String key, Map<String, Long> defaultValue) {
        return FACTORY.ofLongMap(key, defaultValue);
    }

    /**
     * 配置类型: map_string_int64
     */
    public static <K> HconfBuilder<Map<K, Long>> ofLongMap(String key, Map<K, Long> defaultValue, MapKeyParser<K> keyParser) {
        return FACTORY.ofLongMap(key, defaultValue, keyParser);
    }

    /**
     * 配置类型: map_string_double
     */
    public static HconfBuilder<Map<String, Double>> ofDoubleMap(String key, Map<String, Double> defaultValue) {
        return FACTORY.ofDoubleMap(key, defaultValue);
    }

    /**
     * 配置类型: map_string_double
     */
    public static <K> HconfBuilder<Map<K, Double>> ofDoubleMap(String key, Map<K, Double> defaultValue, MapKeyParser<K> keyParser) {
        return FACTORY.ofDoubleMap(key, defaultValue, keyParser);
    }

    /**
     * 配置类型: map_string_string
     */
    public static HconfBuilder<Map<String, String>> ofStringMap(String key, Map<String, String> defaultValue) {
        return FACTORY.ofStringMap(key, defaultValue);
    }

    /**
     * 配置类型: map_string_string
     */
    public static <K> HconfBuilder<Map<K, String>> ofStringMap(String key, Map<K, String> defaultValue, MapKeyParser<K> keyParser) {
        return FACTORY.ofStringMap(key, defaultValue, keyParser);
    }

    /**
     * 配置类型: map_string_bool
     */
    public static HconfBuilder<Map<String, Boolean>> ofBooleanMap(String key, Map<String, Boolean> defaultValue) {
        return FACTORY.ofBooleanMap(key, defaultValue);
    }

    /**
     * 配置类型: map_string_bool
     */
    public static <K> HconfBuilder<Map<K, Boolean>> ofBooleanMap(String key, Map<K, Boolean> defaultValue, MapKeyParser<K> keyParser) {
        return FACTORY.ofBooleanMap(key, defaultValue, keyParser);
    }

    /**
     * 配置类型: map_string_*
     */
    public static <K, V> HconfBuilder<Map<K, V>> ofMap(String key, Map<K, V> defaultValue, MapKeyParser<K> keyParser, ValueParser<V> valueParser) {
        return FACTORY.ofMap(key, defaultValue, keyParser, valueParser);
    }

    /**
     * 内部接口，业务方不要使用
     */
    public static HconfBuilder<Value> ofRawValue(String key) {
        return FACTORY.ofRawValue(key);
    }
}
