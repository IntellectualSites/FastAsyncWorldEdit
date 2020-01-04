package com.boydti.fawe.beta;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public interface IQueueChunk<T extends Future<T>> extends IChunk, Callable<T> {
    /**
     * Reset (defaults to just calling init)
     * @return
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
     * @throws ExecutionException
     * @throws InterruptedException
     */
    default void join() throws ExecutionException, InterruptedException {
        T future = call();
        while (future != null) {
            future = future.get();
        }
        return;
    }
}
