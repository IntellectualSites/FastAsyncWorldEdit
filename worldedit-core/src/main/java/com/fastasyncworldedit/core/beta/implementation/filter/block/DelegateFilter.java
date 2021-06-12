package com.fastasyncworldedit.core.beta.implementation.filter.block;

import com.fastasyncworldedit.core.beta.Filter;
import com.fastasyncworldedit.core.beta.IDelegateFilter;

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