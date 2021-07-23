package com.fastasyncworldedit.core.queue.implementation.filter.block;

import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.IDelegateFilter;

public abstract class DelegateFilter<T extends Filter> implements IDelegateFilter {

    private final Filter parent;

    public DelegateFilter(T parent) {
        this.parent = parent;
    }

    @Override
    public final T getParent() {
        return (T) parent;
    }
}
