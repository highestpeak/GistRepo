package com.highestpeak.gist.redis.util.cache.objects;

/**
 * 可转换成 redis 中 set 类型的对象
 */
public interface SetObject {

    String setKey();

    String member();

}
