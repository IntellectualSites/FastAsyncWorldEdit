package com.fastasyncworldedit.core.queue;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
