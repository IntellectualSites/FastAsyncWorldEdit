package com.sk89q.worldedit.util.task;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LinkedFuture<T extends Future<T>> implements Future<T> {
    private Future<T> task;

    public LinkedFuture(Future<T> task) {
        this.task = task;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return task.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return task.isCancelled();
    }

    @Override
    public boolean isDone() {
        return task.isDone();
    }

    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
        if (task != null) {
            task = task.get();
            if (task != null) {
                return (T) this;
            }
        }
        return null;
    }

    @Override
    public synchronized T get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (task != null) {
            T result = task.get(timeout, unit);
            if (task != null || !task.isDone()) {
                return (T) this;
            }
            task = null;

        }
        return null;
    }
}
