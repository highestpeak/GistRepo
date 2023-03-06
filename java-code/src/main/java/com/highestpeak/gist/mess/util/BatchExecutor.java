package com.highestpeak.gist.mess.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.collect.Sets;

import lombok.NonNull;

/**
 * 分批执行器
 *
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-05
 */
@SuppressWarnings("unused")
public class BatchExecutor {

    public static final int DEFAULT_BATCH_SIZE = 500;

    /**
     * 可中断执行的批量操作
     *
     * @param consumer 函数的返回值如果为 false，则终止后续的执行
     */
    public static <T> void interruptibleBatchExecute(@NonNull Collection<T> items, int batchSize, @NonNull Function<Collection<T>, Boolean> consumer) {
        Set<T> t = Sets.newHashSetWithExpectedSize(batchSize);
        for (T item : items) {
            if (t.size() == batchSize) {
                if (!consumer.apply(t)) {
                    return;
                }
                t.clear();
            }
            t.add(item);
        }
        if (!t.isEmpty()) {
            consumer.apply(t);
        }
    }

    /**
     * 批量执行
     */
    public static <T> void batchExecute(@NonNull Collection<T> items, int batchSize, @NonNull Consumer<Collection<T>> consumer) {
        if (items.isEmpty()) {
            return;
        }
        if (items.size() <= batchSize) {
            consumer.accept(items);
            return;
        }
        Set<T> t = Sets.newHashSetWithExpectedSize(batchSize);
        for (T item : items) {
            if (t.size() == batchSize) {
                consumer.accept(t);
                t.clear();
            }
            t.add(item);
        }
        if (!t.isEmpty()) {
            consumer.accept(t);
        }
    }

    /**
     * 批量执行 任务列表可以被调用方继续添任务
     */
    public static <T> void batchExecute(@NonNull Queue<T> items, int batchSize, @NonNull Consumer<Collection<T>> consumer) {
        Set<T> t = new HashSet<>(batchSize);
        while (!items.isEmpty()) {
            t.add(items.poll());
            if (items.size() == 0 || t.size() == batchSize) {
                consumer.accept(t);
                t.clear();
            }
        }
    }

}
