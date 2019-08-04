package com.boydti.fawe.beta.filters;

import com.boydti.fawe.beta.FilterBlock;

public class CountFilter extends ForkedFilter<CountFilter> {

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
}
