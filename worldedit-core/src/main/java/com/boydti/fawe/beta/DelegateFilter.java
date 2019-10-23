package com.boydti.fawe.beta;

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