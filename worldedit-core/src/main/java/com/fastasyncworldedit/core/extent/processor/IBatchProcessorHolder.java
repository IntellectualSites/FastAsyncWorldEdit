package com.fastasyncworldedit.core.extent.processor;

import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.worldedit.extent.Extent;

import java.util.concurrent.Future;

/**
 * Holds a batch processor
 * (Join and remove operations affect the held processor)
 */
public interface IBatchProcessorHolder extends IBatchProcessor {

    IBatchProcessor getProcessor();

    IBatchProcessor getPostProcessor();

    /**
     * Set the held processor
     */
    void setProcessor(IBatchProcessor set);

    void setPostProcessor(IBatchProcessor set);

    @Override
    default IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        return getProcessor().processSet(chunk, get, set);
    }

    @Override
    default void postProcessSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        getPostProcessor().postProcessSet(chunk, get, set);
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
    default IBatchProcessor joinPost(IBatchProcessor other) {
        setPostProcessor(getPostProcessor().joinPost(other));
        return this;
    }

    @Override
    default <T extends IBatchProcessor> IBatchProcessor remove(Class<T> clazz) {
        setProcessor(getProcessor().remove(clazz));
        return this;
    }

}
