package com.highestpeak.gist.redis.util.cache.objects;

/**
 * 可转换成 redis 中 zset 类型的对象
 */
public interface ZSetObject {

    String zSetKey();

    String zSetMember();

    Double zSetScore();

}
