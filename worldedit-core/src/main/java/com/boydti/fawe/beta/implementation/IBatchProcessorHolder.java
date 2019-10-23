package com.boydti.fawe.beta.implementation;

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
    default IChunkSet processBatch(IChunk chunk, IChunkGet get, IChunkSet set) {
        return getProcessor().processBatch(chunk, get, set);
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
