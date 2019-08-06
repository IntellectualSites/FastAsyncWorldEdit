package com.boydti.fawe.beta;

public class DelegateFilter<T extends Filter> implements IDelegateFilter {

    private final Filter parent;

    public DelegateFilter(T parent) {
        this.parent = parent;
    }

    @Override
    public T getParent() {
        return (T) parent;
    }

    @Override
    public Filter newInstance(Filter other) {
        return new DelegateFilter(other);
    }
}
