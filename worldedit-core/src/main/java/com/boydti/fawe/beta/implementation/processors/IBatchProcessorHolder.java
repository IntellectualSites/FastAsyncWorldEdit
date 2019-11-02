package com.boydti.fawe.beta.implementation.processors;

import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.sk89q.worldedit.extent.Extent;

/**
 * Holds a batch processor
 * (Join and remove operations affect the held processor)
 */
public interface IBatchProcessorHolder extends IBatchProcessor {
    IBatchProcessor getProcessor();

    /**
     * set the held processor
     * @param set
     */
    void setProcessor(IBatchProcessor set);

    @Override
    default IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        return getProcessor().processSet(chunk, get, set);
    }

    @Override
    default boolean processGet(int chunkX, int chunkZ) {
        return getProcessor().processGet(chunkX, chunkZ);
    }

    @Override
    default Extent construct(Extent child) {
        return getProcessor().construct(child);
    }

    @Override
    default IBatchProcessor join(IBatchProcessor other) {
        setProcessor(getProcessor().join(other));
        return this;
    }

    @Override
    default <T extends IBatchProcessor> IBatchProcessor remove(Class<T> clazz) {
        setProcessor(getProcessor().remove(clazz));
        return this;
    }
}
