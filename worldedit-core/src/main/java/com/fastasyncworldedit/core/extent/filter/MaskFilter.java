package com.fastasyncworldedit.core.extent.filter;

import com.fastasyncworldedit.core.extent.filter.block.DelegateFilter;
import com.fastasyncworldedit.core.extent.filter.block.FilterBlock;
import com.fastasyncworldedit.core.internal.simd.VectorFacade;
import com.fastasyncworldedit.core.internal.simd.SimdSupport;
import com.fastasyncworldedit.core.internal.simd.VectorizedFilter;
import com.fastasyncworldedit.core.internal.simd.VectorizedMask;
import com.fastasyncworldedit.core.queue.Filter;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filter with an attached Mask used for deciding whether a block is eligible for being applied to.
 *
 * @param <T> Parent which extends Filter
 */
public class MaskFilter<T extends Filter> extends DelegateFilter<T> {

    final Mask mask;
    final AtomicInteger changes;

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

    public static class VectorizedMaskFilter<T extends VectorizedFilter> extends MaskFilter<T> implements VectorizedFilter {

        private final VectorizedMask vectorizedMask;

        public VectorizedMaskFilter(final T other, final Mask root) {
            super(other, root);
            this.vectorizedMask = Objects.requireNonNull(SimdSupport.vectorizedTargetMask(root), "invalid vectorizable mask");
        }

        public VectorizedMaskFilter(final T other, final Mask root, AtomicInteger changes) {
            super(other, root, changes);
            this.vectorizedMask = Objects.requireNonNull(SimdSupport.vectorizedTargetMask(root), "invalid vectorizable mask");
        }

        @Override
        public void applyVector(final VectorFacade get, final VectorFacade set, final VectorMask<Short> mask) {
            final T parent = getParent();
            final VectorSpecies<Short> species = mask.vectorSpecies();
            VectorMask<Short> masked = this.vectorizedMask.compareVector(set, get, species);
            ShortVector before = set.getOrZero(masked.vectorSpecies());
            parent.applyVector(get, set, mask.and(masked));
            ShortVector after = set.getOrZero(masked.vectorSpecies());
            VectorMask<Short> changed = after.compare(VectorOperators.NE, before);
            this.changes.getAndAdd(changed.trueCount());
        }

        @Override
        public MaskFilter<?> newInstance(final Filter other) {
            if (other instanceof VectorizedFilter o) {
                return new VectorizedMaskFilter<>(o, mask);
            }
            return super.newInstance(other);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Filter fork() {
            return new VectorizedMaskFilter<>((T) getParent().fork(), mask.copy(), changes);
        }

    }

}
