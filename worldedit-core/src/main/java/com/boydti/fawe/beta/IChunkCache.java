package com.boydti.fawe.beta;

import org.jetbrains.annotations.Range;

/**
 * IGetBlocks may be cached by the WorldChunkCache so that it can be used between multiple
 * IQueueExtents - avoids conversion between a palette and raw data on every block get
 */
public interface IChunkCache<T> extends Trimable {
    T get(@Range(from = 0, to = 15) int chunkX, @Range(from = 0, to = 15) int chunkZ);

    @Override
    default boolean trim(boolean aggressive) {
        return false;
    }
}
