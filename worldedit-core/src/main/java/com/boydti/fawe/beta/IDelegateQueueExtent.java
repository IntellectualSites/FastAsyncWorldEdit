package com.boydti.fawe.beta;

import com.boydti.fawe.beta.implementation.WorldChunkCache;

import java.util.concurrent.Future;

/**
 * Delegate for IQueueExtent
 */
public interface IDelegateQueueExtent extends IQueueExtent {
    IQueueExtent getParent();

    @Override
    default void init(final WorldChunkCache cache) {
        getParent().init(cache);
    }

    @Override
    default IChunk getCachedChunk(final int x, final int z) {
        return getParent().getCachedChunk(x, z);
    }

    @Override
    default Future<?> submit(final IChunk chunk) {
        return getParent().submit(chunk);
    }

    @Override
    default IChunk create(final boolean full) {
        return getParent().create(full);
    }

    @Override
    default IChunk wrap(final IChunk root) {
        return getParent().wrap(root);
    }

    @Override
    default void flush() {
        getParent().flush();
    }

    @Override
    default boolean trim(final boolean aggressive) {
        return getParent().trim(aggressive);
    }
}
