package com.fastasyncworldedit.core.internal.simd;

import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.implementation.blocks.DataArray;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorMask;

public interface VectorizedMask<T> {

    default void processChunks(IChunk chunk, IChunkGet get, IChunkSet set) {
        for (int layer = get.getMinSectionPosition(); layer <= get.getMaxSectionPosition(); layer++) {
            final DataArray sectionSet = set.loadIfPresent(layer);
            if (sectionSet == null) {
                continue;
            }
            final DataArray sectionGet = get.load(layer);
            processSection(layer, sectionSet, sectionGet);
        }
    }

    default void processSection(int layer, DataArray set, DataArray get) {
        set.processSet(get, this::processVector);
    }

    /**
     * {@return the set vector with all lanes that do not match this mask set to 0}
     *
     * @param set the set vector
     * @param get the get vector
     */
    default Vector<T> processVector(Vector<T> set, Vector<T> get) {
        return set.blend(BlockTypesCache.ReservedIDs.__RESERVED__, compareVector(set, get).not());
    }

    /**
     * {@return a mask with all lanes set that match this mask}
     *
     * @param set the set vector
     * @param get the get vector
     */
    VectorMask<T> compareVector(Vector<T> set, Vector<T> get);

}
