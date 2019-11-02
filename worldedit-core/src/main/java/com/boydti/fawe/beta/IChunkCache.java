package com.boydti.fawe.beta;

import com.boydti.fawe.beta.Trimable;

/**
 * IGetBlocks may be cached by the WorldChunkCache so that it can be used between multiple
 * IQueueExtents - avoids conversion between palette and raw data on every block get
 */
public interface IChunkCache<T> extends Trimable {
    T get(int chunkX, int chunkZ);

    @Override
    default boolean trim(boolean aggressive) {
        return false;
    }
}
