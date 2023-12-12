package com.fastasyncworldedit.core.queue;

/**
 * IGetBlocks may be cached by the WorldChunkCache so that it can be used between multiple
 * IQueueExtents - avoids conversion between a palette and raw data on every block get
 */
public interface IChunkCache<T> extends Trimable {

    T get(int chunkX, int chunkZ);

    @Override
    default boolean trim(boolean aggressive) {
        return false;
    }

}
