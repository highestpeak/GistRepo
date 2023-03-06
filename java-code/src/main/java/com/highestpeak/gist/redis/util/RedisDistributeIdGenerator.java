package com.highestpeak.gist.redis.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import com.highestpeak.gist.common.help.HostInfo;
import com.highestpeak.gist.facade.api.JedisCluster;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 分布式环境下的全局唯一 id 生成器，总长度 64 位，参考 SnowFlake 算法 todo 雪花算法
 * 1[reserved] + 41[timestamp] + 10[machine id] + 12[sequence]
 * 额外增加 id 缓存，处理服务器发生时钟回拨时（NTP同步），从缓存发号
 * todo https://github.com/hengyunabc/redis-id-generator
 * todo https://github.com/intenthq/icicle
 *
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-04
 */
@SuppressWarnings("unused,UnusedReturnValue")
@Slf4j
public class RedisDistributeIdGenerator {

    private static final int YEAR = 2023;
    /**
     * 时间偏移量，从 2023 年 1 月 1 日 零点开始
     */
    public static final long TIMESTAMP_EPOCH = LocalDateTime.of(YEAR, 1, 1, 0, 0, 0)
            .toInstant(ZoneOffset.of("+8"))
            .toEpochMilli();

    private static final long SEQUENCE_LEN = 12L;
    private static final long MACHINE_ID_LEN = 10L;
    private static final long SEQUENCE_MASK = (1 << SEQUENCE_LEN) - 1;
    private static final long MACHINE_ID_MASK = (1 << MACHINE_ID_LEN) - 1;
    private static final long MACHINE_ID_LEFT_SHIFT = SEQUENCE_LEN;
    private static final long TIMESTAMP_ID_LEFT_SHIFT = MACHINE_ID_LEFT_SHIFT + MACHINE_ID_LEN;

    private final long machineId;
    private volatile long lastTime;
    private final AtomicInteger seqIncr = new AtomicInteger(0);
    /**
     * 时钟回拨时，缓存一段 id 号
     */
    private final RingBuffer<Long> ringBuffer = new RingBuffer<>();

    public RedisDistributeIdGenerator(MachineIdProvider machineIdProvider) {
        // 构造时，生成 machineId
        machineId = machineIdProvider.genMachineId();
    }

    public Long generateId() {
        long time = System.currentTimeMillis();

        // lastTime > time 说明发生了时钟回拨，尝试使用缓存的 id
        if (lastTime > time) {
            synchronized (ringBuffer) {
                Long cacheId = ringBuffer.get();
                if (cacheId != null) {
                    return cacheId;
                }
            }
            throw new IllegalStateException("Clock is moving backwards, last time is " + lastTime
                    + " milliseconds, current time is " + time + " milliseconds");
        }

        // 毫秒切换时，更新一次缓存的 id
        if (lastTime < time) {
            // 额外保存一个 id
            saveSupplyId(lastTime);
            lastTime = time;
        }

        return generateId0(time);
    }

    private Long generateId0(long time) {
        long seq = seqIncr.incrementAndGet() & SEQUENCE_MASK;
        return ((time - TIMESTAMP_EPOCH) << TIMESTAMP_ID_LEFT_SHIFT) | (machineId << MACHINE_ID_LEFT_SHIFT) | seq;
    }

    private void saveSupplyId(long time) {
        synchronized (ringBuffer) {
            // 丢弃最老，覆盖一个最新的
            if (ringBuffer.full()) {
                ringBuffer.get();
            }
            ringBuffer.put(generateId0(time));
        }
    }

    /**
     * 简单的循环缓存，用于时钟回拨时，获取缓存的 id
     */
    private static class RingBuffer<T> {
        private static final int DEFAULT_SIZE = 1024;
        private final Object[] buffer;
        private int head = 0;
        private int tail = 0;
        private final int bufferSize;

        public RingBuffer() {
            this.bufferSize = DEFAULT_SIZE;
            this.buffer = new Object[bufferSize];
        }

        public RingBuffer(int initSize) {
            this.bufferSize = initSize;
            this.buffer = new Object[bufferSize];
        }

        private boolean empty() {
            return head == tail;
        }

        private boolean full() {
            return (tail + 1) % bufferSize == head;
        }

        public void clear() {
            Arrays.fill(buffer, null);
            this.head = 0;
            this.tail = 0;
        }

        public boolean put(T v) {
            if (full()) {
                return false;
            }
            buffer[tail] = v;
            tail = (tail + 1) % bufferSize;
            return true;
        }

        public T get() {
            if (empty()) {
                return null;
            }
            //noinspection unchecked
            T result = (T) buffer[head];
            head = (head + 1) % bufferSize;
            return result;
        }
    }

    /**
     * 生成唯一的机器码
     */
    public interface MachineIdProvider {
        long genMachineId();
    }

    /**
     * 基于 Redis 的机器码生成服务
     * 启动线程，定时同步心跳
     */
    public static class RedisMachineIdProvider implements MachineIdProvider {
        private static final String REDIS_MACHINE_ID_HOSTS_PREFIX = "id:gen:machine:id:hosts:";
        private static final String REDIS_MACHINE_ID_INCR_PREFIX = "id:gen:machine:id:incr:";
        private static final ScheduledExecutorService SCH_EXE = Executors.newScheduledThreadPool(1,
                new CustomizableThreadFactory("redis-machine-id-heartbeat"));

        private final String bizName;
        private final JedisCluster redisClient;
        private long machineId;

        public RedisMachineIdProvider(String bizName, JedisCluster redisClient) {
            this.bizName = bizName;
            this.redisClient = redisClient;
            initMachineId();
            SCH_EXE.scheduleWithFixedDelay(this::keepAliveWorkId, 2, 2, TimeUnit.MINUTES);
        }

        @Override
        public long genMachineId() {
            return machineId;
        }

        private String getMachineIdHostsKey() {
            return REDIS_MACHINE_ID_HOSTS_PREFIX + bizName;
        }

        private String getMachineIdIncrKey() {
            return REDIS_MACHINE_ID_INCR_PREFIX + bizName;
        }

        private void initMachineId() {
            String hostName = HostInfo.getHostName();
            Map<String, WorkIdVo> hostWorkIdMap = loadAlreadyWordIdHosts();
            WorkIdVo workIdVo = hostWorkIdMap.get(hostName);
            if (workIdVo == null) { // 说明当前是新节点
                long newWorkId = genNewWorkId(hostWorkIdMap);
                if (newWorkId < 0) {
                    throw new IllegalStateException("init workId fail");
                }
                workIdVo = new WorkIdVo(newWorkId, System.currentTimeMillis());
                redisClient.get().hset(getMachineIdHostsKey(), hostName, workIdVo.toStoreLine());
            }
            this.machineId = workIdVo.workId;
            log.info("initMachineId bizName: {}, machineId: {}", bizName, machineId);
            keepAliveWorkId();
        }

        private void keepAliveWorkId() {
            try {
                // keepAlive
                String hostName = HostInfo.getHostName();
                WorkIdVo workIdVo = new WorkIdVo(this.machineId, System.currentTimeMillis());
                redisClient.get().hset(getMachineIdHostsKey(), hostName, workIdVo.toStoreLine());
                log.debug("keepAliveWorkId bizName: {}, machineId: {}", bizName, machineId);

                // 删除过期 2小时的数据
                long now = System.currentTimeMillis();
                Map<String, WorkIdVo> hostWorkIdMap = loadAlreadyWordIdHosts();
                for (Map.Entry<String, WorkIdVo> entry : hostWorkIdMap.entrySet()) {
                    WorkIdVo workIdVoItem = entry.getValue();
                    if (now - workIdVoItem.getUpdateTs() > Duration.ofHours(2).toMillis()) {
                        redisClient.get().hdel(getMachineIdHostsKey(), entry.getKey());
                        log.info("keepAliveWorkId delete expire host: {}", entry.getKey());
                    }
                }
            } catch (Exception e) {
                log.warn("keepAliveWorkId execute exception.", e);
            }
        }

        private Map<String, WorkIdVo> loadAlreadyWordIdHosts() {
            Map<String, String> hostWorkIdMap = redisClient.get().hgetAll(getMachineIdHostsKey());
            Map<String, WorkIdVo> wordIdVoMap = new HashMap<>();
            for (Map.Entry<String, String> entry : hostWorkIdMap.entrySet()) {
                WorkIdVo workIdVo = WorkIdVo.parseStoreLine(entry.getValue());
                if (workIdVo != null) {
                    wordIdVoMap.put(entry.getKey(), workIdVo);
                }
            }
            return wordIdVoMap;
        }

        private long genNewWorkId(Map<String, WorkIdVo> hostWorkIdMap) {
            for (int i = 0; i < MACHINE_ID_MASK; i++) {
                // 重试时，分配新 workId 时，避免产生冲突
                long newWorkId = redisClient.get().incr(getMachineIdIncrKey()) & MACHINE_ID_MASK;
                if (hostWorkIdMap.values().stream().filter(Objects::nonNull).anyMatch(workIdVo -> workIdVo.getWorkId() == newWorkId)) {
                    continue;
                }
                return newWorkId;
            }
            return -1;
        }
    }

    @Data
    private static class WorkIdVo {
        private final long workId;
        private final long updateTs;

        public String toStoreLine() {
            return workId + ":" + updateTs;
        }

        public static WorkIdVo parseStoreLine(String line) {
            if (line == null) {
                return null;
            }
            String[] items = line.split(":");
            if (items.length != 2) {
                log.error("parse wrong workVo line: {}", line);
                return null;
            }
            long parseWorkId = Long.parseLong(items[0]);
            long parseUpdateTs = Long.parseLong(items[1]);
            return new WorkIdVo(parseWorkId, parseUpdateTs);
        }
    }

}
