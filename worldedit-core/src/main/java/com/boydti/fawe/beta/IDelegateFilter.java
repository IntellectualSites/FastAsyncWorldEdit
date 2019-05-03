package com.boydti.fawe.beta;

public interface IDelegateFilter extends Filter {
    Filter getParent();

    @Override
    default Filter appliesChunk(int cx, int cz) {
        Filter copy = getParent().appliesChunk(cx, cz);
        if (copy == null) return null;
        if (copy != getParent()) {
            return newInstance(copy);
        } else {
            return this;
        }
    }

    @Override
    default IChunk applyChunk(IChunk chunk) {
        return getParent().applyChunk(chunk);
    }

    @Override
    default Filter appliesLayer(IChunk chunk, int layer) {
        Filter copy = getParent().appliesLayer(chunk, layer);
        if (copy == null) return null;
        if (copy != getParent()) {
            return newInstance(copy);
        } else {
            return this;
        }
    }

    @Override
    default void applyBlock(FilterBlock block) {
        getParent().applyBlock(block);
    }

    @Override
    default void finishChunk(IChunk chunk) {
        getParent().finishChunk(chunk);
    }

    @Override
    default void join() {
        getParent().join();
    }

    @Override
    default Filter fork() {
        return newInstance(getParent().fork());
    }

    Filter newInstance(Filter other);
}
