package com.sk89q.worldedit.function.mask;

import com.boydti.fawe.beta.implementation.filter.block.DelegateFilter;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.implementation.filter.block.FilterBlock;

import java.util.function.Supplier;

public class MaskFilter<T extends Filter> extends DelegateFilter<T> {
    private final Supplier<Mask> supplier;
    private final Mask mask;

    public MaskFilter(T other, Mask mask) {
        this(other, () -> mask);
    }

    public MaskFilter(T other, Supplier<Mask> supplier) {
        this(other, supplier, supplier.get());
    }

    public MaskFilter(T other, Supplier<Mask> supplier, Mask root) {
        super(other);
        this.supplier = supplier;
        this.mask = root;
    }

    @Override
    public void applyBlock(FilterBlock block) {
        if (mask.test(block)) {
            getParent().applyBlock(block);
        }
    }

    @Override
    public MaskFilter newInstance(Filter other) {
        return new MaskFilter<>(other, supplier);
    }
}
