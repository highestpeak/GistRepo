package com.highestpeak.gist.db.util;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import lombok.Getter;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-04-16
 */
@Getter
public class DBEntityIterator<T extends DBEntity> implements Iterator<T> {

    private static final int DEFAULT_PAGE_SIZE = 1000;

    private final int pageSize;
    private long offset;
    private final BaseMapper<T> entityMapper;
    private Iterator<T> iterator;
    private final Consumer<LambdaQueryWrapper<T>> condition;
    private final Class<T> entityClass;

    public DBEntityIterator(BaseMapper<T> entityMapper, Class<T> entityClass) {
        this(entityMapper, entityClass, query -> {});
    }

    public DBEntityIterator(BaseMapper<T> entityMapper, Class<T> entityClass, Consumer<LambdaQueryWrapper<T>> condition) {
        this(entityMapper, entityClass, condition, 0);
    }

    public DBEntityIterator(BaseMapper<T> entityMapper, Class<T> entityClass, Consumer<LambdaQueryWrapper<T>> condition, long offset) {
        this(entityMapper, entityClass, condition, offset, DEFAULT_PAGE_SIZE);
    }

    public DBEntityIterator(BaseMapper<T> entityMapper, Class<T> entityClass, Consumer<LambdaQueryWrapper<T>> condition, long offset, int pageSize) {
        this.entityMapper = entityMapper;
        this.entityClass = entityClass;
        this.condition = condition;
        this.offset = offset;
        this.pageSize = pageSize;
        iterator = dump().iterator();
    }

    private List<T> dump() {
        LambdaQueryWrapper<T> query = Wrappers.lambdaQuery(entityClass)
                // future: 可以支持其他的排序方法 ?
                .orderByAsc(T::getId)
                .gt(T::getId, offset)
                .func(condition)
                .last(" limit " + pageSize);
        List<T> result = entityMapper.selectList(query);

        if (CollectionUtils.isNotEmpty(result)) {
            offset = result.get(result.size() - 1).getId();
        }
        return result;
    }

    @Override
    public boolean hasNext() {
        if (!iterator.hasNext()) {
            iterator = dump().iterator();
        }
        return iterator.hasNext();
    }

    @Override
    public T next() {
        if (hasNext()) {
            return iterator.next();
        }
        throw new NoSuchElementException();
    }
}