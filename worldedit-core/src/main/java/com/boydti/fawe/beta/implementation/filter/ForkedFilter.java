package com.boydti.fawe.beta.implementation.filter;

import com.boydti.fawe.beta.Filter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ForkedFilter<T extends ForkedFilter<T>> implements Filter {

    protected final Map<Thread, T> children;

    public ForkedFilter(T root) {
        if (root != null) {
            children = root.children;
        } else {
            children = new ConcurrentHashMap<>();
            children.put(Thread.currentThread(), (T) this);
        }
    }

    @Override
    public final Filter fork() {
        return children.computeIfAbsent(Thread.currentThread(), thread -> init());
    }

    public abstract T init();

    @Override
    public void join() {
        for (Map.Entry<Thread, T> entry : children.entrySet()) {
            T filter = entry.getValue();
            if (filter != this) {
                join(filter);
            }
        }
        children.clear();
    }

    public abstract void join(T filter);
}
