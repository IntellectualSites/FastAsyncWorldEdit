package com.fastasyncworldedit.core.extent.filter;

import com.fastasyncworldedit.core.extent.filter.block.DelegateFilter;
import com.fastasyncworldedit.core.extent.filter.block.FilterBlock;
import com.fastasyncworldedit.core.internal.simd.VectorizedFilter;
import com.fastasyncworldedit.core.queue.Filter;
import jdk.incubator.vector.ShortVector;

/**
 * Filter which links two Filters together for single-filter-input operations.
 *
 * @param <T> Parent which extends Filter
 * @param <S> Child which extends Filter
 */
public sealed class LinkedFilter<T extends Filter, S extends Filter> extends DelegateFilter<T> {

    private final S child;

    @SuppressWarnings({"unchecked", "rawtypes"}) // we defeated the type system
    public static <T extends Filter, S extends Filter> LinkedFilter<? extends T, ? extends S> of(T parent, S child) {
        if (parent instanceof VectorizedFilter p && child instanceof VectorizedFilter c) {
            return new VectorizedLinkedFilter(p, c);
        }
        return new LinkedFilter<>(parent, child);
    }

    public LinkedFilter(T parent, S child) {
        super(parent);
        this.child = child;
    }

    public S getChild() {
        return this.child;
    }

    @Override
    public void applyBlock(FilterBlock block) {
        this.getParent().applyBlock(block);
        this.getChild().applyBlock(block);
    }

    @Override
    public LinkedFilter<? extends LinkedFilter<T, S>, ? extends Filter> newInstance(Filter other) {
        return new LinkedFilter<>(this, other);
    }

    private final static class VectorizedLinkedFilter<T extends VectorizedFilter, S extends VectorizedFilter>
            extends LinkedFilter<T, S> implements VectorizedFilter {

        public VectorizedLinkedFilter(final T parent, final S child) {
            super(parent, child);
        }

        @Override
        public ShortVector applyVector(final ShortVector get, final ShortVector set) {
            ShortVector res = getParent().applyVector(get, set);
            return getChild().applyVector(get, res);
        }

        @Override
        public LinkedFilter<? extends LinkedFilter<T, S>, Filter> newInstance(Filter other) {
            if (other instanceof VectorizedFilter o) {
                return new VectorizedLinkedFilter(this, o);
            }
            return new LinkedFilter<>(this, other);
        }
    }

}
