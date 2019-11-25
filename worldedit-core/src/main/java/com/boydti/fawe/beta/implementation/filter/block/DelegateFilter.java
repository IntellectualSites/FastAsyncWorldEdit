package com.boydti.fawe.beta.implementation.filter.block;

import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.IDelegateFilter;

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