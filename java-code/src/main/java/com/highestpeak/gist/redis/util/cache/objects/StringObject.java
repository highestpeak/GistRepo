package com.highestpeak.gist.redis.util.cache.objects;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * 可转换成 redis 中 String 类型的对象
 */
public interface StringObject {

    String stringKey();

    byte[] serialize();

    void deSerialize(byte[] message) throws InvalidProtocolBufferException;

}
