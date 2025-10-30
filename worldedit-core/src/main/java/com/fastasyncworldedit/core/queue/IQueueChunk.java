package com.fastasyncworldedit.core.queue;

import com.fastasyncworldedit.core.queue.implementation.chunk.ChunkHolder;
import com.fastasyncworldedit.core.queue.implementation.chunk.WrapperChunk;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@ApiStatus.Internal
public interface IQueueChunk<T extends Future<T>> extends IChunk, Callable<T> {

    /**
     * Reset (defaults to just calling init)
     */
    @Override
    default IQueueChunk<T> reset() {
        init(null, getX(), getZ());
        return this;
    }

    /**
     * Set a new {@link WrapperChunk} that allows prevention of a {@link ChunkHolder} instance being cached "locally" whilst it
     * has been called/submitted, causing issues with processing/postprocessing, etc.
     * If a wrapper has already been set, throws {@link IllegalStateException} as there should be no circumstance for us to set
     * a new wrapper (does nothing if attempting to set the same wrapper).
     *
     * @param parentWrapper wrapper wrapping this {@link ChunkHolder instance}
     * @throws IllegalStateException if there is already a wrapper set and a new wrapper instance is attempted to be se
     * @since TODO
     */
    void setWrapper(WrapperChunk<?> parentWrapper);

    /**
     * Invalidate the {@link WrapperChunk} if present.
     *
     * @since TODO
     */
    void invalidateWrapper();

    /**
     * Apply the queued changes to the world containing this chunk.
     * <p>The future returned may return another future. To ensure completion keep calling {@link
     * Future#get()} on each result.</p>
     *
     * @return Future
     */
    @Override
    T call();

    /**
     * Call and join
     * - Should be done async, if at all
     */
    default void join() throws ExecutionException, InterruptedException {
        T future = call();
        while (future != null) {
            future = future.get();
        }
    }

    /**
     * Get if the thank has any running tasks, locked locks, etc.
     */
    default boolean hasRunning() {
        return false;
    }

    /**
     * Prevent set operations to the chunk, should typically be used when a chunk is submitted before the edit is necessarily
     * completed.
     */
    default void lockSet() {
    }

}
