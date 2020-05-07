package com.boydti.fawe.beta.implementation.filter;

import com.boydti.fawe.beta.implementation.filter.block.DelegateFilter;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.implementation.filter.block.FilterBlock;
import com.sk89q.worldedit.function.mask.Mask;

import java.util.function.Supplier;

/**
 * Filter with an attached Mask used for deciding whether a block is eligible for being applied to.
 *
 * @param <T> Parent which extends Filter
 */
public class MaskFilter<T extends Filter> extends DelegateFilter<T> {
    private final Supplier<Mask> supplier;
    private final Mask mask;
    private int changes;

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
        if (mask.test(block, block)) {
            getParent().applyBlock(block);
            this.changes++;
        }
    }

    /**
     * Get the number of blocks which passed the Mask test and were applied to
     *
     * @return number of blocks which passed the Mask test and were applied to
     */
    public int getBlocksApplied(){
        return this.changes;
    }

    @Override
    public MaskFilter newInstance(Filter other) {
        return new MaskFilter<>(other, supplier);
    }
}
