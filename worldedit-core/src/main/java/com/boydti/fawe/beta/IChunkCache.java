package com.boydti.fawe.beta;

/**
 * IGetBlocks may be cached by the WorldChunkCache so that it can be used between multiple
 * IQueueExtents - avoids conversion between a palette and raw data on every block get
 */
public interface IChunkCache<T> extends Trimable {

    @Override
    default boolean trim(boolean aggressive) {
        return false;
    }
}
