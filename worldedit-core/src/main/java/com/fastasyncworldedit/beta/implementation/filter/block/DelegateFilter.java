package com.fastasyncworldedit.beta.implementation.filter.block;

import com.fastasyncworldedit.beta.Filter;
import com.fastasyncworldedit.beta.IDelegateFilter;

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