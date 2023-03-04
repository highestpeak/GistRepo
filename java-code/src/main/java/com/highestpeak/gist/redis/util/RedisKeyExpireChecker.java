package com.highestpeak.gist.redis.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.highestpeak.gist.common.HashUtil;
import com.highestpeak.gist.common.Pair;
import com.highestpeak.gist.common.conf.ConfigMapper;
import com.highestpeak.gist.common.conf.Hconf;
import com.highestpeak.gist.common.conf.Hconfs;
import com.highestpeak.gist.common.help.RedisTuple;
import com.highestpeak.gist.concurrent.util.BlockingDelayBufferTrigger;
import com.highestpeak.gist.facade.api.JedisCluster;
import com.highestpeak.gist.facade.api.RedisCommands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * unit test & usage example. see RedisKeyExpireCheckerTest <br/>
 * 功能: 在 x duration 后执行 checkId 对应的任务的逻辑
 * <p/>
 * 特点: <br/>
 * 1. 基于 redis 实现, 分布式收集到同一个 redis 队列 (PS: 基于内存的实现为 {@link BlockingDelayBufferTrigger}) <br/>
 * 2. 不能支撑过大的 qps，如果有 qps 过大的场景，使用基于内存的 {@link BlockingDelayBufferTrigger}
 * <p/>
 * 实现要点: <br/>
 * 1. 分片 <br/>
 * 2. 分布式配置中心 配置消费实例限制
 * <p/>
 *
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-03
 */
@SuppressWarnings("unused")
@Slf4j
public class RedisKeyExpireChecker {

    /**
     * 常量值
     */
    private static final String REDIS_EXPIRE_CHECKER_PRE = "redis-expire-checker-";
    private static final String REDIS_LOCK_PRE = "lock:nx:ex:checker:";

    private static final Hconf<Integer> SCREEN_SHOT_IDLE_CHECK_MAX_LOOP = Hconfs.ofInteger(
            "xxx.yyy.redis_checker_idle_check_max_loop", 100).build();

    /**
     * queue -> [instanceFullNamePrefix1, instanceFullNamePrefix]
     */
    private static final Hconf<Map<String, Set<String>>> REDIS_QUEUE_CONSUMER_INSTANCE = Hconfs.ofStringMap(
            "xxx.yyy.redis_queue_consumer_instance", Collections.emptyMap())
            .mapper((ConfigMapper<Map<String, String>, Map<String, Set<String>>>) stringStringMap -> {
                if (MapUtils.isEmpty(stringStringMap)) {
                    return Collections.emptyMap();
                }
                Map<String, Set<String>> dataMap = Maps.newHashMapWithExpectedSize(stringStringMap.size());
                for (Map.Entry<String, String> entry : stringStringMap.entrySet()) {
                    Set<String> instanceFullNamePrefixSet = Sets.newHashSet(Splitter.on(",").trimResults().omitEmptyStrings()
                            .split(entry.getValue()));
                    dataMap.put(entry.getKey(), instanceFullNamePrefixSet);
                }
                return dataMap;
            }).build();

    /**
     * 本地全局变量: 不同的 checker 的定时线程池维护的地方
     */
    private static final Map<String, ScheduledExecutorService> SCH_EXEC_MAP = new ConcurrentHashMap<>();

    /**
     * 初始化参数
     */
    private final String uniqName;
    private final int shard;
    private final JedisCluster redisCluster;
    private final Executor executor;
    private final Consumer<String> consumer;
    private final int readLoopCount;
    private final boolean enableAutoRetry;
    private final Duration autoRetryDuration;

    /**
     * 封装参数
     */
    @Getter
    private final RedisObjectReadWriter redisObjectReadWriter;
    private final String checkIdLockPre;
    /**
     * 当前实例的完整名称 <br/>
     * serviceName####hostName（使用 '####' 进行拼接，避免有特殊字符）
     */
    private final String instanceFullName;
    private final String[] sortSetNames;

    /**
     * 内部控制变量
     */
    private final AtomicInteger shardOffset = new AtomicInteger(0);

    private RedisKeyExpireChecker(Builder builder) {
        this.uniqName = builder.uniqName;
        this.shard = builder.shard <= 0 ? 12 : builder.shard;
        this.redisCluster = builder.redisCluster;
        this.executor = builder.executor == null ? MoreExecutors.directExecutor() : builder.executor;
        this.consumer = builder.consumer;
        this.readLoopCount = builder.readLoopCount > 0 ? builder.readLoopCount : Math.max(1, shard / 3);
        this.enableAutoRetry = builder.enableAutoRetry;
        this.autoRetryDuration = builder.autoRetryDuration;

        this.redisObjectReadWriter = new RedisObjectReadWriter(innerKey -> innerKey, builder.redisCluster);
        this.checkIdLockPre = REDIS_LOCK_PRE + builder.uniqName + ":";
        this.instanceFullName = getInstanceFullName();
        log.info("instanceFullName: {}", instanceFullName);
        // initSortSetNames
        this.sortSetNames = new String[shard];
        for (int i = 0; i < shard; i++) {
            sortSetNames[i] = REDIS_EXPIRE_CHECKER_PRE + uniqName + "-" + i;
        }

        if (!builder.disableChecker) {
            int targetIntervalSeconds = builder.checkIntervalSeconds < 1 ? 30 : builder.checkIntervalSeconds;
            ScheduledExecutorService schExe = SCH_EXEC_MAP.computeIfAbsent(
                    uniqName,
                    nameFormat -> Executors.newScheduledThreadPool(
                            1, new ThreadFactoryBuilder().setNameFormat(nameFormat).build()
                    )
            );
            schExe.scheduleWithFixedDelay(() -> checkExpire(false), targetIntervalSeconds, targetIntervalSeconds, TimeUnit.SECONDS);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private String getInstanceFullName() {
        // WARNING: imply you code here. 根据实际情况获取服务名、泳道名、主机和实例名
        String serviceName = null;
        String laneId = null;
        String hostName = null;
        return String.join("####",
                StringUtils.defaultIfBlank(serviceName, "null"),
                StringUtils.defaultIfBlank(laneId, "null"),
                StringUtils.defaultIfBlank(hostName, "null")
        );
    }

    private FirstData peekFirst() {
        Set<String> whiteHostSet = REDIS_QUEUE_CONSUMER_INSTANCE.get().get(uniqName);
        if (CollectionUtils.isNotEmpty(whiteHostSet) && whiteHostSet.stream().noneMatch(instanceFullName::startsWith)) {
            // 配置了白名单，且白名单前缀不匹配自己时，不消费数据
            return null;
        }

        for (int i = 0; i < readLoopCount; i++) {
            String sortSetName = getSortSetName(getShardAndNext());
            // 仅取第一个元素
            Set<RedisTuple> targetList = getRedisClient().zrangeWithScores(sortSetName, 0, 0);
            if (targetList == null || targetList.isEmpty()) {
                log.debug("uniqName: {}, checkExpire no idle checkId to process", uniqName);
                continue;
            }

            RedisTuple firstData = targetList.iterator().next();
            long firstEleExpireWait = firstData.getScore().longValue() - System.currentTimeMillis();
            if (firstEleExpireWait > 0) {
                log.debug("uniqName: {}, checkExpire first checkId: {} not time to process", uniqName, firstData.getElement());
                continue;
            }

            String checkId = firstData.getElement();
            return new FirstData(sortSetName, checkId);
        }
        return null;
    }

    private int getShardAndNext() {
        do {
            int last = shardOffset.get();
            int next = last + 1;
            if (next >= shard) {
                next = 0;
            }
            if (shardOffset.compareAndSet(last, next)) {
                return last;
            }
        } while (true);
    }

    /**
     * 定时任务会定时调用该方法检测，不用自行调用
     */
    private Optional<String> checkExpire(boolean manualCheck) {
        Set<String> checkIdSeen = new HashSet<>();
        try {
            int loop = 0;
            do {
                FirstData firstData = peekFirst();
                if (firstData == null) {
                    log.debug("uniqName: {}, checkExpire not found expire firstData", uniqName);
                    break;
                }
                // 避免 enableAutoRetry 时，请求重新加入队首后的空跑问题
                if (!checkIdSeen.add(firstData.getCheckId())) {
                    continue;
                }

                Optional<String> checkIdOptional = oneCheckIdOptional(firstData);
                if (manualCheck) {
                    return checkIdOptional;
                } else {
                    checkIdOptional.ifPresent(docId ->
                            executor.execute(() -> consumer.accept(docId))
                    );
                }
                // 移除节点产生并发时，默认循环 100 次
            } while (++loop < SCREEN_SHOT_IDLE_CHECK_MAX_LOOP.get());
        } catch (Throwable t) {
            log.error("RedisKeyExpireChecker checkExpire exception. uniqName: {}", uniqName, t);
        }
        return Optional.empty();
    }

    private Optional<String> oneCheckIdOptional(FirstData firstData) {
        String checkId = firstData.getCheckId();
        if (enableAutoRetry) {
            String flag = getRedisClient().set(checkIdLockPre + checkId, "1", "NX", "EX", 3);
            // 一段时间后会自动重试，业务执行成功后，需要手动调用 RedisKeyExpireChecker#deleteKey，否则一直重试
            if ("OK".equals(flag)) {
                getRedisClient().zadd(firstData.getSortSetName(), System.currentTimeMillis() + autoRetryDuration.toMillis(), checkId);
                log.info("uniqName: {}, checkExpire process checkId: {}, autoRetry after: {}", uniqName, checkId, autoRetryDuration.toMillis());
                return Optional.of(checkId);
            } else {
                log.info("uniqName: {}, checkExpire first checkId: {} process by other instance", uniqName, checkId);
            }
        } else {
            // 不进行自动重试时，服务重启时业务需要容忍部分请求丢失。
            // 使用 zrem 不需要后续额外调用 RedisKeyExpireChecker#deleteKey，业务上使用更简单
            Long remCount = getRedisClient().zrem(firstData.getSortSetName(), checkId);
            // 当前节点移除成功
            if (remCount == 1) {
                log.info("uniqName: {}, checkExpire process checkId: {}", uniqName, checkId);
                return Optional.of(checkId);
            } else {
                log.info("uniqName: {}, checkExpire first checkId: {} process by other instance", uniqName, checkId);
            }
        }
        return Optional.empty();
    }

    private RedisCommands getRedisClient() {
        return redisCluster.get();
    }

    private String getSortSetName(int shardId) {
        return sortSetNames[shardId];
    }

    private int hashShardId(String checkId) {
        long hashCode = HashUtil.hashStrToLong(checkId);
        int shareId = Math.toIntExact(hashCode % shard);
        if (shareId < 0) {
            shareId += shard;
        }
        return shareId;
    }

    // ======================================================== check item operate ======================================================== //

    /**
     * 当空闲一段时间后，触发检查，到期则执行 consumer <br/>
     * 比如 redis 的部分 key 写入了过期时间：<br/>
     * 如果 key 持续更新，会覆盖过期检查的延迟时间； <br/>
     * 如果 key 空闲，延迟到期后进行检查 <br/>
     *
     * @param checkId  需要检查的唯一标记
     * @param duration 需要延迟检查的时间
     */
    public void checkWhenIdleDelay(String checkId, Duration duration) {
        // 如果延迟时间是负数不影响，统一处理逻辑
        int shareId = hashShardId(checkId);
        String sortSetName = getSortSetName(shareId);
        // todo 现在是分数是到期时间，写一个实现是分数是写入时间，然后到期后按照需要，从 Hconf 读出配置的过期时间长度，然后从队列消费需要消费的数据
        long execTs = System.currentTimeMillis() + duration.toMillis();
        getRedisClient().zadd(sortSetName, execTs, checkId);
    }

    /**
     * 固定间隔时间后，触发检查，到期则执行 consumer <br/>
     * 提交请求时，会判断 redis 是否有对应 key：<br/>
     * key不存在或请求延迟时间小于 redis 中的记录，使用请求覆盖 redis 中记录；<br/>
     * 如果请求延迟时间大于 redis 中已有请求，丢弃当前请求 <br/>
     * 比如用户持续编辑文档时，需要每 3 分钟生成一次缩略图。<br/>
     */
    public void checkWhenFixDelay(String checkId, Duration duration) {
        // 如果延迟时间是负数不影响，统一处理逻辑
        int shareId = hashShardId(checkId);
        String sortSetName = getSortSetName(shareId);
        long execTs = System.currentTimeMillis() + duration.toMillis();
        Double redisExecTs = getRedisClient().zscore(sortSetName, checkId);
        if (redisExecTs == null || execTs < redisExecTs.longValue()) {
            getRedisClient().zadd(sortSetName, execTs, checkId);
        }
    }

    /**
     * 用于延迟重试场景主动移除任务，任务发起调用时，加入延迟检查. <br/>
     * 如果任务正常执行成功，主动移除任务；如果超时后任务未成功执行，重新发起调用
     */
    public void deleteKey(String checkId) {
        int shareId = hashShardId(checkId);
        String sortSetName = getSortSetName(shareId);
        getRedisClient().zrem(sortSetName, checkId);
        String attachKey = uniqName + checkId;
        getRedisClient().del(attachKey);
    }

    /**
     * 检查一个key是否存在
     */
    public boolean isExists(String checkId) {
        int shareId = hashShardId(checkId);
        String sortSetName = getSortSetName(shareId);
        return Objects.nonNull(
                getRedisClient().zrank(sortSetName, checkId)
        );
    }

    // ======================================================= check queue operate ======================================================== //

    /**
     * 总任务数
     */
    public long totalTaskCount() {
        long totalSize = 0;
        for (int i = 0; i < shard; i++) {
            String sortSetName = getSortSetName(i);
            Long shareSize = getRedisClient().zcard(sortSetName);
            if (shareSize != null) {
                totalSize += shareSize;
            }
        }
        return totalSize;
    }

    /**
     * 手动获取队列中下一个该执行的任务
     */
    public Optional<String> checkExpireManual() {
        return checkExpire(true);
    }

    // ============================================================== 管理接口 ============================================================== //

    public List<Pair<String, Long>> peekAllCheckKey() {
        List<Pair<String, Long>> keyList = new ArrayList<>();
        for (String sortSetKey : sortSetNames) {
            Set<RedisTuple> tupleSet = getRedisClient().zrangeWithScores(sortSetKey, 0, -1);
            for (RedisTuple tuple : tupleSet) {
                keyList.add(new Pair<>(tuple.getElement(), tuple.getScore().longValue()));
            }
        }
        keyList.sort(Comparator.comparing(Pair::getValue));
        return keyList;
    }

    public void removeChecker() {
        for (String sortSetKey : sortSetNames) {
            getRedisClient().del(sortSetKey);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Accessors(chain = true)
    @Setter
    public static class Builder {

        /**
         * 队列名称
         */
        private String uniqName;
        /**
         * redis 分片数量. 建议根据 redis 集群的实例数量来配置.
         */
        private int shard;
        /**
         * 定时线程检查 redis 队列的时间间隔
         */
        private int checkIntervalSeconds;
        /**
         * redis 实例
         */
        private JedisCluster redisCluster;
        /**
         * 任务执行线程池
         */
        private Executor executor;
        /**
         * 任务执行方法
         */
        private Consumer<String> consumer;
        /**
         * 是否启用定时线程池轮询 redis 队首 <br/>
         * 未关闭 checker 选项时， consumer 必须设置
         */
        private boolean disableChecker;
        /**
         * 请求是否支持自动重试，当 checkId 触发执行时，会自动重新把任务添加到排序集合中，设置下次触发执行的时间为 autoRetryDuration 之后
         */
        private boolean enableAutoRetry;
        /**
         * 配合 enableAutoRetry 使用，当 enableAutoRetry = true 时，不允许为空. <br/>
         * 自动重试参数间隔必须大于 5 秒，避免设置过短造成过大压力 <br/>
         * 建议默认设置为 3-5 分钟，如果业务上需要秒级的重试，可以使用 checkWhenIdleDelay 覆盖自动写入的下次执行时间
         */
        private Duration autoRetryDuration;
        /**
         * 查询数据时，当前队列为空时，额外轮询的分片处理
         */
        private int readLoopCount;

        public RedisKeyExpireChecker build() {
            Preconditions.checkNotNull(uniqName, "must define uniqName");
            Preconditions.checkNotNull(redisCluster, "must define redis instance");
            if (!disableChecker) { // 未关闭 checker 选项时， consumer 必须设置
                Preconditions.checkNotNull(consumer, "must define check consumer when closeCheck was false");
            }
            if (enableAutoRetry) {
                Preconditions.checkNotNull(autoRetryDuration, "autoRetryDuration can not null when enableAutoRetry=true");
                // 自动重试参数间隔必须大于 5 秒，避免设置过短造成过大压力。建议默认设置为 3-5 分钟，如果
                Preconditions.checkArgument(autoRetryDuration.getSeconds() > 5, "autoRetryDuration must gt 5 seconds");
            }
            return new RedisKeyExpireChecker(this);
        }
    }

    /**
     * 用于检查队首元素
     */
    @AllArgsConstructor
    @Getter
    private static class FirstData {
        private final String sortSetName;
        private final String checkId;
    }

}
