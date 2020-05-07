package com.boydti.fawe.beta.implementation.filter;

import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.implementation.filter.block.DelegateFilter;
import com.boydti.fawe.beta.implementation.filter.block.FilterBlock;

/**
 * Filter which links two Filters together for single-filter-input operations.
 *
 * @param <T> Parent which extends Filter
 * @param <S> Child which extends Filter
 */
public final class LinkedFilter<T extends Filter, S extends Filter> extends DelegateFilter<T> {

    private final S child;

    public LinkedFilter(T parent, S child){
        super(parent);
        this.child = child;
    }

    public S getChild(){
        return this.child;
    }

    @Override
    public void applyBlock(FilterBlock block) {
        this.getParent().applyBlock(block);
        this.getChild().applyBlock(block);
    }

    @Override
    public LinkedFilter<LinkedFilter<T,S>, Filter> newInstance(Filter other) {
        return new LinkedFilter<>(this, other);
    }
}
