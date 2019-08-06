package com.boydti.fawe.beta;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ChunkFuture implements Future<Void> {

    private final IChunk chunk;
    private volatile boolean cancelled;
    private volatile boolean done;

    public ChunkFuture(IChunk chunk) {
        this.chunk = chunk;
    }

    public IChunk getChunk() {
        return chunk;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        cancelled = true;
        return !done;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public Void get() throws InterruptedException, ExecutionException {
        synchronized (chunk) {
            if (!done) {
                this.wait();
            }
        }
        return null;
    }

    @Override
    public Void get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        synchronized (chunk) {
            if (!done) {
                this.wait(unit.toMillis(timeout));
                if (!done) {
                    throw new TimeoutException();
                }
            }
        }
        return null;
    }
}
