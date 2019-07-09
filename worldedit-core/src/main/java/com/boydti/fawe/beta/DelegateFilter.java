package com.boydti.fawe.beta;

public abstract class DelegateFilter implements IDelegateFilter {
    private final Filter parent;

    public DelegateFilter(Filter parent) {
        this.parent = parent;
    }
    @Override
    public Filter getParent() {
        return parent;
    }
}
