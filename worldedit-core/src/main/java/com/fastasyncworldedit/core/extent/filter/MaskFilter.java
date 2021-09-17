package com.fastasyncworldedit.core.extent.filter;

import com.fastasyncworldedit.core.extent.filter.block.DelegateFilter;
import com.fastasyncworldedit.core.extent.filter.block.FilterBlock;
import com.fastasyncworldedit.core.queue.Filter;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filter with an attached Mask used for deciding whether a block is eligible for being applied to.
 *
 * @param <T> Parent which extends Filter
 */
public class MaskFilter<T extends Filter> extends DelegateFilter<T> {

    private final Mask mask;
    private final AtomicInteger changes;

    public MaskFilter(T other, Mask root) {
        this(other, root, new AtomicInteger());
    }

    public MaskFilter(T other, Mask root, AtomicInteger changes) {
        super(other);
        this.mask = root;
        this.changes = changes;
    }

    @Override
    public void applyBlock(FilterBlock block) {
        if (mask instanceof AbstractExtentMask) {
            if (((AbstractExtentMask) mask).test(block.getExtent(), block)) {
                getParent().applyBlock(block);
                this.changes.incrementAndGet();
            }
        } else if (mask.test(block)) {
            getParent().applyBlock(block);
            this.changes.incrementAndGet();
        }
    }

    /**
     * Get the number of blocks which passed the Mask test and were applied to
     *
     * @return number of blocks which passed the Mask test and were applied to
     */
    public int getBlocksApplied() {
        return this.changes.get();
    }

    @Override
    public MaskFilter<?> newInstance(Filter other) {
        return new MaskFilter<>(other, mask);
    }

    @Override
    public Filter fork() {
        return new MaskFilter<>(getParent().fork(), mask.copy(), changes);
    }

}
