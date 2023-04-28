package com.highestpeak.gist.concurrent.util;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-04-16
 */
@Slf4j
public final class BlockingDelayBufferTrigger<KEY, VAL> {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition queueEmpty = lock.newCondition();
    private final Condition queueFull = lock.newCondition();
    private final int capacity;

    @SuppressWarnings("FieldCanBeLocal")
    private final Thread dispatcherThread;
    private volatile boolean running = true;

    private final HashMap<KEY, DelayWrapper<KEY, VAL>> dataMap;
    private final PriorityQueue<DelayWrapper<KEY, VAL>> priorityQueue;
    private final BiConsumer<KEY, VAL> upToTimeConsumer;
    private final BiFunction<VAL, VAL, VAL> dupValFunc;
    private final KeyAffinityExecutor<KEY> keyAffinityExecutor;

    private BlockingDelayBufferTrigger(String threadName, int capacity, int executorCount,
            BiConsumer<KEY, VAL> upToTimeConsumer, BiFunction<VAL, VAL, VAL> dupValFunc) {
        this.capacity = capacity;
        this.dataMap = new HashMap<>();
        this.priorityQueue = new PriorityQueue<>();
        this.upToTimeConsumer = upToTimeConsumer;
        this.dupValFunc = dupValFunc;

        // 线程名称
        String executorThreadName = StringUtils.defaultIfBlank(threadName, "blocking-delay-buffer-trigger");
        // 区分不同的线程名称
        if (!executorThreadName.contains("%d")) {
            executorThreadName += "-%d";
        }

        // 使用可以去重的亲缘性线程会更好，避免大任务多次并行执行
        this.keyAffinityExecutor = ExecutorsExUtils.newSkipDuplicateExecutor(executorCount, executorCount, executorThreadName);

        String dispatcherThreadName = "dispatcher-" + executorThreadName;
        this.dispatcherThread = new Thread(() -> {
            while (running) {
                try {
                    dispatcher();
                } catch (Exception e) {
                    log.warn("check start exception.", e);
                }
            }
        }, dispatcherThreadName);
        this.dispatcherThread.setDaemon(true);
        this.dispatcherThread.start();
    }

    private String buildOperationName() {
        return "asyncExecute/" + Thread.currentThread().getName() + "/BufferDispatcher";
    }

    private void dispatcher() throws Exception {
        DelayWrapper<KEY, VAL> upToTimeObj = peekAndRemoveUpToTime();
        try {
            if (upToTimeObj.getMdcContext() != null) {
                // dispatcher 线程没有上下文，MDC 中没有需要执行后恢复的内容
                MDC.setContextMap(upToTimeObj.getMdcContext());
            }
            Scope.supplyWithExistScope(upToTimeObj.getScope(), executeWithKtrace(upToTimeObj));
        } finally {
            // dispatcher 线程没有上下文，MDC 直接清理
            MDC.clear();
        }
    }

    /**
     * 参考 {@link com.kuaishou.infra.ktrace.sdk.async.KtraceCallable#call}, 传递 ktrace
     */
    private ThrowableSupplier<Void, Exception> executeWithKtrace(DelayWrapper<KEY, VAL> upToTimeObj) {
        return () -> {
            if (upToTimeObj.getSnapshot() == null) {
                keyAffinityExecutor.executeEx(upToTimeObj.getKey(), () -> upToTimeConsumer.accept(upToTimeObj.getKey(), upToTimeObj.getVal()));
            } else {
                Span localSpan = null;
                String operationName = buildOperationName();
                try {
                    localSpan = createLocalSpanAndContinued(operationName, upToTimeObj.getSnapshot())
                            .setComponentName(LOCAL_COMPONENT_NAME_THREAD_SWITCH);
                } catch (Throwable t) {
                    KtracePerfUtils.perfInstrumentFailed(COMPONENT_TYPE_LOCAL, "", operationName);
                    log.error("[fail safe] [ktrace] create span failed!", t);
                }

                try {
                    keyAffinityExecutor.executeEx(upToTimeObj.getKey(), () -> upToTimeConsumer.accept(upToTimeObj.getKey(), upToTimeObj.getVal()));
                } catch (Throwable t) {
                    if (localSpan != null) {
                        localSpan.errorOccurred().log(t);
                    }
                    throw t;
                } finally {
                    if (null != localSpan) {
                        stopSpan(localSpan);
                    }
                }
            }
            return null;
        };
    }

    public void shutdown() {
        this.running = false;
        try {
            keyAffinityExecutor.close();
        } catch (Exception e) {
            log.warn("shutdown exception.", e);
        }
    }

    /**
     * 如果在延迟过程中，有新请求，不改变延迟排序，确保 1 分钟执行一次
     */
    public void add(KEY key, VAL param, long delay, TimeUnit unit) {
        // 当前请求的执行时间
        long execTs = System.currentTimeMillis();
        if (delay > 0) {
            execTs += unit.toMillis(delay);
        }

        lock.lock();
        try {
            // 如果已经包含该key，1: 使用更短的执行时间，支持插队；2：对携带的对象进行 merge
            if (dataMap.containsKey(key)) {
                DelayWrapper<KEY, VAL> oldDelay = dataMap.get(key);
                VAL oldVal = oldDelay.getVal();
                VAL targetVal = dupValFunc.apply(oldVal, param); // 基于 oldVal 和 newVal 合并新的 val
                if (execTs < oldDelay.getExecTs()) { // 新增的延迟更低，删除老元素，插入新元素。确保 priorityQueue 能触发排序
                    priorityQueue.remove(oldDelay);
                    dataMap.remove(key);

                    DelayWrapper<KEY, VAL> delayWrapper = new DelayWrapper<>(execTs, key, targetVal);
                    priorityQueue.add(delayWrapper);
                    dataMap.put(key, delayWrapper);
                    log.info("addToDelay found short delay request, key:{}, delay:{}, unit:{}", key, delay, unit);
                    // 新增加的元素在队首，唤醒分发线程
                    if (delayWrapper == priorityQueue.peek()) {
                        queueEmpty.signal();
                    }
                } else {
                    oldDelay.setVal(targetVal);
                    log.debug("addToDelay distinct key:{}, delay:{}, unit:{}", key, delay, unit);
                }
                return;
            }

            while (dataMap.size() >= capacity) {
                try {
                    boolean status = queueFull.await(10, TimeUnit.SECONDS);
                    if (!status) { // false 由于超时导致， 需要重新等待
                        log.warn("queueFull always full wait 10 seconds. key: {}", key);
                    }
                } catch (InterruptedException e) {
                    log.info("thread was interrupt by system, need close. key: {}", key);
                    break;
                }
            }

            DelayWrapper<KEY, VAL> delayWrapper = new DelayWrapper<>(execTs, key, param);
            priorityQueue.add(delayWrapper);
            dataMap.put(key, delayWrapper);
            // 新增加的元素在队首，唤醒分发线程
            if (delayWrapper == priorityQueue.peek()) {
                queueEmpty.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取队首最老的一个元素，确认是否满足延时时间
     */
    private DelayWrapper<KEY, VAL> peekAndRemoveUpToTime() {
        try {
            DelayWrapper<KEY, VAL> wrapper;
            do {
                lock.lock();
                try {
                    boolean awaitStatus = true;
                    wrapper = priorityQueue.peek();
                    if (wrapper == null) {
                        awaitStatus = queueEmpty.await(10, TimeUnit.SECONDS);
                    } else {
                        long upToTimeMills = wrapper.getExecTs();
                        long needWait = upToTimeMills - System.currentTimeMillis();
                        if (needWait > 0) { // 支持插队唤醒
                            awaitStatus = queueEmpty.await(needWait, TimeUnit.MILLISECONDS);
                        }
                    }
                    log.debug("peekAndRemoveUpToTime awaitStatus={}, wrapper is {} null", awaitStatus, wrapper == null ? "" : "not");
                } finally {
                    lock.unlock();
                }
            } while (wrapper == null || wrapper.getExecTs() > System.currentTimeMillis());

            // 移除队首，返回数据，触发执行
            lock.lock();
            try {
                priorityQueue.remove(wrapper);
                dataMap.remove(wrapper.getKey());
                if (dataMap.size() < capacity) {
                    queueFull.signal();
                }
            } finally {
                lock.unlock();
            }
            return wrapper;
        } catch (InterruptedException ie) {
            throw new RuntimeException("thread was interrupt by system, need close");
        } catch (Exception e) {
            log.error("peek exception.", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 用于包装请求的参数
     */
    @Getter
    private static class DelayWrapper<KEY, VAL> implements Comparable<DelayWrapper<KEY, VAL>> {
        private final long execTs;
        private final KEY key;
        @Setter
        private VAL val;
        private Map<String, String> mdcContext;

        public DelayWrapper(long execTs, KEY key, VAL val) {
            this.execTs = execTs;
            this.key = key;
            this.val = val;
        }

        @Override
        public int compareTo(@NonNull DelayWrapper<KEY, VAL> o) {
            return ComparisonChain.start()
                    .compare(this.execTs, o.execTs)
                    .compare(this.key.toString(), o.key.toString())
                    .result();
        }

    }

    public static <KEY, VAL> Builder<KEY, VAL> newBuilder() {
        return new Builder<>();
    }

    /**
     * Builder 方式构建对象
     */
    @Accessors(fluent = true)
    @Setter
    public static class Builder<KEY, VAL> {
        private String threadName;
        private int capacity;
        private BiConsumer<KEY, VAL> upToTimeConsumer;
        private BiFunction<VAL, VAL, VAL> dupValConsumer;
        /**
         * 默认5个执行线程
         */
        private int executorCount = 5;

        public BlockingDelayBufferTrigger<KEY, VAL> build() {
            Preconditions.checkArgument(capacity > 100, "capacity is too small");
            return new BlockingDelayBufferTrigger<>(threadName,
                    capacity, executorCount, upToTimeConsumer, dupValConsumer
            );
        }
    }


}