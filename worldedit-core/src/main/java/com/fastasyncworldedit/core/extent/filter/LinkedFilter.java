package com.fastasyncworldedit.core.extent.filter;

import com.fastasyncworldedit.core.extent.filter.block.FilterBlock;
import com.fastasyncworldedit.core.internal.simd.VectorFacade;
import com.fastasyncworldedit.core.internal.simd.VectorizedFilter;
import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.IChunk;
import com.sk89q.worldedit.regions.Region;
import jdk.incubator.vector.VectorMask;
import org.jetbrains.annotations.Nullable;

/**
 * Filter which links two Filters together for single-filter-input operations. Left filter is operated first.
 *
 * @param <L> Left filter
 * @param <R> Right filter
 */
public sealed class LinkedFilter<L extends Filter, R extends Filter> implements Filter {

    private final L left;
    private final R right;

    public LinkedFilter(L left, R right) {
        this.left = left;
        this.right = right;
    }

    @SuppressWarnings({"unchecked", "rawtypes"}) // we defeated the type system
    public static <L extends Filter, R extends Filter> LinkedFilter<? extends L, ? extends R> of(L left, R right) {
        if (left instanceof VectorizedFilter l && right instanceof VectorizedFilter r) {
            return new VectorizedLinkedFilter(l, r);
        }
        return new LinkedFilter<>(left, right);
    }

    public L getLeft() {
        return this.left;
    }

    public R getRight() {
        return this.right;
    }

    @Override
    public <T extends IChunk> T applyChunk(T chunk, @Nullable Region region) {
        chunk = getLeft().applyChunk(chunk, region);
        return getRight().applyChunk(chunk, region);
    }

    @Override
    public void applyBlock(FilterBlock block) {
        getLeft().applyBlock(block);
        getRight().applyBlock(block);
    }

    @Override
    public void finishChunk(IChunk chunk) {
        getLeft().finishChunk(chunk);
        getRight().finishChunk(chunk);
    }

    @Override
    public Filter fork() {
        return new LinkedFilter<>(getLeft().fork(), getRight().fork());
    }

    @Override
    public void join() {
        getLeft().join();
        getRight().join();
    }

    private final static class VectorizedLinkedFilter<L extends VectorizedFilter, R extends VectorizedFilter>
            extends LinkedFilter<L, R> implements VectorizedFilter {

        public VectorizedLinkedFilter(final L left, final R right) {
            super(left, right);
        }

        @Override
        public void applyVector(final VectorFacade get, final VectorFacade set, final VectorMask<Short> mask) {
            getLeft().applyVector(get, set, mask);
            getRight().applyVector(get, set, mask);
        }

        @Override
        public Filter fork() {
            return new VectorizedLinkedFilter<>((L) getLeft().fork(), (R) getRight().fork());
        }

    }

}
