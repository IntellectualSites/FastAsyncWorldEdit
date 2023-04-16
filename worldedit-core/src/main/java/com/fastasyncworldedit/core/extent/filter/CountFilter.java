package com.fastasyncworldedit.core.extent.filter;

import com.fastasyncworldedit.core.extent.filter.block.FilterBlock;
import com.fastasyncworldedit.core.internal.simd.VectorFacade;
import com.fastasyncworldedit.core.internal.simd.VectorizedFilter;
import jdk.incubator.vector.VectorMask;

public class CountFilter extends ForkedFilter<CountFilter> implements VectorizedFilter {

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
    public void applyVector(final VectorFacade get, final VectorFacade set, final VectorMask<Integer> mask) {
        total += mask.trueCount();
    }

}
