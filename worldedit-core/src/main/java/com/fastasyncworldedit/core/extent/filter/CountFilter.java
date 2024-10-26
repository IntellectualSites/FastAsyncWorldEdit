package com.fastasyncworldedit.core.extent.filter;

import com.fastasyncworldedit.core.extent.filter.block.FilterBlock;
import com.fastasyncworldedit.core.internal.simd.VectorizedFilter;
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorMask;

public class CountFilter<T> extends ForkedFilter<CountFilter<T>> implements VectorizedFilter<T> {

    private int total;

    public CountFilter() {
        super(null);
    }

    private CountFilter(CountFilter root) {
        super(root);
    }

    @Override
    public CountFilter init() {
        return new CountFilter(this);
    }

    @Override
    public void join(CountFilter filter) {
        this.total += filter.getTotal();
    }

    @Override
    public final void applyBlock(FilterBlock block) {
        total++;
    }

    public int getTotal() {
        return total;
    }

    @Override
    public Vector<T> applyVector(final Vector<T> get, final Vector<T> set, VectorMask<T> mask) {
        total += mask.trueCount();
        return set;
    }

}
