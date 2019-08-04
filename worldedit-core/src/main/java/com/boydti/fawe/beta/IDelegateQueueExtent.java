package com.boydti.fawe.beta;

import com.boydti.fawe.beta.implementation.WorldChunkCache;
import java.util.concurrent.Future;

/**
 * Delegate for IQueueExtent
 */
public interface IDelegateQueueExtent extends IQueueExtent {

    IQueueExtent getParent();

    @Override
    default void init(WorldChunkCache cache) {
        getParent().init(cache);
    }

    @Override
    default IChunk getCachedChunk(int x, int z) {
        return getParent().getCachedChunk(x, z);
    }

    @Override
    default Future<?> submit(IChunk chunk) {
        return getParent().submit(chunk);
    }

    @Override
    default IChunk create(boolean isFull) {
        return getParent().create(isFull);
    }

    @Override
    default IChunk wrap(IChunk root) {
        return getParent().wrap(root);
    }

    @Override
    default void flush() {
        getParent().flush();
    }

    @Override
    default boolean trim(boolean aggressive) {
        return getParent().trim(aggressive);
    }
}
