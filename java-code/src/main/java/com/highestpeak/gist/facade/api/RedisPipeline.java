package com.highestpeak.gist.facade.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.highestpeak.gist.common.help.RedisTuple;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-05
 */
@SuppressWarnings({"ALL", "UnusedReturnValue"})
public interface RedisPipeline {

    PipelineResponse<Long> append(String key, String value);

    PipelineResponse<List<String>> blpop(String arg);

    PipelineResponse<List<String>> brpop(String arg);

    PipelineResponse<Long> decr(String key);

    PipelineResponse<Long> decrBy(String key, long integer);

    PipelineResponse<Long> del(String key);

    PipelineResponse<String> echo(String string);

    PipelineResponse<Boolean> exists(String key);

    PipelineResponse<Long> expire(String key, int seconds);

    PipelineResponse<Long> pexpire(String key, long milliseconds);

    PipelineResponse<Long> expireAt(String key, long unixTime);

    PipelineResponse<Long> pexpireAt(String key, long millisecondsTimestamp);

    PipelineResponse<String> get(String key);

    PipelineResponse<Boolean> getbit(String key, long offset);

    PipelineResponse<String> getrange(String key, long startOffset, long endOffset);

    PipelineResponse<String> getSet(String key, String value);

    PipelineResponse<Long> hdel(String key, String... field);

    PipelineResponse<Boolean> hexists(String key, String field);

    PipelineResponse<String> hget(String key, String field);

    PipelineResponse<Map<String, String>> hgetAll(String key);

    PipelineResponse<Long> hincrBy(String key, String field, long value);

    PipelineResponse<Set<String>> hkeys(String key);

    PipelineResponse<Long> hlen(String key);

    PipelineResponse<List<String>> hmget(String key, String... fields);

    PipelineResponse<String> hmset(String key, Map<String, String> hash);

    PipelineResponse<Long> hset(String key, String field, String value);

    PipelineResponse<Long> hsetnx(String key, String field, String value);

    PipelineResponse<List<String>> hvals(String key);

    PipelineResponse<Long> incr(String key);

    PipelineResponse<Long> incrBy(String key, long integer);

    PipelineResponse<String> lindex(String key, long index);

    // PipelineResponse<Long> linsert(String key, BinaryClient.LIST_POSITION where, String pivot, String value);

    PipelineResponse<Long> llen(String key);

    PipelineResponse<String> lpop(String key);

    PipelineResponse<Long> lpush(String key, String... string);

    PipelineResponse<Long> lpushx(String key, String... string);

    PipelineResponse<List<String>> lrange(String key, long start, long end);

    PipelineResponse<Long> lrem(String key, long count, String value);

    PipelineResponse<String> lset(String key, long index, String value);

    PipelineResponse<String> ltrim(String key, long start, long end);

    PipelineResponse<Long> move(String key, int dbIndex);

    PipelineResponse<Long> persist(String key);

    PipelineResponse<String> rpop(String key);

    PipelineResponse<Long> rpush(String key, String... string);

    PipelineResponse<Long> rpushx(String key, String... string);

    PipelineResponse<Long> sadd(String key, String... member);

    PipelineResponse<Long> scard(String key);

    PipelineResponse<Boolean> sismember(String key, String member);

    PipelineResponse<String> set(String key, String value);

    PipelineResponse<Boolean> setbit(String key, long offset, boolean value);

    PipelineResponse<String> setex(String key, int seconds, String value);

    PipelineResponse<Long> setnx(String key, String value);

    PipelineResponse<Long> setrange(String key, long offset, String value);

    PipelineResponse<Set<String>> smembers(String key);

    PipelineResponse<List<String>> sort(String key);

    // PipelineResponse<List<String>> sort(String key, SortingParams sortingParameters);

    PipelineResponse<String> spop(String key);

    PipelineResponse<Set<String>> spop(String key, long count);

    PipelineResponse<String> srandmember(String key);

    PipelineResponse<Long> srem(String key, String... member);

    PipelineResponse<Long> strlen(String key);

    PipelineResponse<String> substr(String key, int start, int end);

    PipelineResponse<Long> ttl(String key);

    PipelineResponse<String> type(String key);

    PipelineResponse<Long> zadd(String key, double score, String member);

    // PipelineResponse<Long> zadd(String key, double score, String member, ZAddParams params);

    PipelineResponse<Long> zadd(String key, Map<String, Double> scoreMembers);

    // PipelineResponse<Long> zadd(String key, Map<String, Double> scoreMembers, ZAddParams params);

    PipelineResponse<Long> zcard(String key);

    PipelineResponse<Long> zcount(String key, double min, double max);

    PipelineResponse<Double> zincrby(String key, double score, String member);

    // PipelineResponse<Double> zincrby(String key, double score, String member, ZIncrByParams params);

    PipelineResponse<Set<String>> zrange(String key, long start, long end);

    PipelineResponse<Set<String>> zrangeByScore(String key, double min, double max);

    PipelineResponse<Set<String>> zrangeByScore(String key, String min, String max);

    PipelineResponse<Set<String>> zrangeByScore(String key, double min, double max, int offset, int count);

    PipelineResponse<Set<RedisPipeline>> zrangeByScoreWithScores(String key, double min, double max);

    PipelineResponse<Set<RedisPipeline>> zrangeByScoreWithScores(String key, double min, double max, int offset, int count);

    PipelineResponse<Set<String>> zrevrangeByScore(String key, double max, double min);

    PipelineResponse<Set<String>> zrevrangeByScore(String key, String max, String min);

    PipelineResponse<Set<String>> zrevrangeByScore(String key, double max, double min, int offset, int count);

    PipelineResponse<Set<RedisPipeline>> zrevrangeByScoreWithScores(String key, double max, double min);

    PipelineResponse<Set<RedisPipeline>> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count);

    PipelineResponse<Set<RedisPipeline>> zrangeWithScores(String key, long start, long end);

    PipelineResponse<Long> zrank(String key, String member);

    PipelineResponse<Long> zrem(String key, String... member);

    PipelineResponse<Long> zremrangeByRank(String key, long start, long end);

    PipelineResponse<Long> zremrangeByScore(String key, double start, double end);

    PipelineResponse<Set<String>> zrevrange(String key, long start, long end);

    PipelineResponse<Set<RedisTuple>> zrevrangeWithScores(String key, long start, long end);

    PipelineResponse<Long> zrevrank(String key, String member);

    PipelineResponse<Double> zscore(String key, String member);

    PipelineResponse<Long> zlexcount(final String key, final String min, final String max);

    PipelineResponse<Set<String>> zrangeByLex(final String key, final String min, final String max);

    PipelineResponse<Set<String>> zrangeByLex(final String key, final String min, final String max,
                                              final int offset, final int count);

    PipelineResponse<Set<String>> zrevrangeByLex(final String key, final String max, final String min);

    PipelineResponse<Set<String>> zrevrangeByLex(final String key, final String max, final String min,
                                                 final int offset, final int count);

    PipelineResponse<Long> zremrangeByLex(final String key, final String start, final String end);

    PipelineResponse<Long> bitcount(String key);

    PipelineResponse<Long> bitcount(String key, long start, long end);

    PipelineResponse<Long> pfadd(final String key, final String... elements);

    PipelineResponse<Long> pfcount(final String key);

    PipelineResponse<List<Long>> bitfield(String key, String... arguments);

    PipelineResponse<Long> pttl(String key);

    PipelineResponse<String> set(String key, String value, String nxxx, String expx, int time);

    PipelineResponse<String> psetex(String key, long milliseconds, String value);

    PipelineResponse<Long> zremrangeByScore(String key, String start, String end);

}
