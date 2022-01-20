package com.fastasyncworldedit.core.util;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MultiFuture implements Future<Object[]> {

    private final List<Future<?>> futures;

    public MultiFuture(List<Future<?>> futures) {
        this.futures = futures;
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        return futures.stream().allMatch(f -> f.cancel(mayInterruptIfRunning));
    }

    @Override
    public boolean isCancelled() {
        return futures.stream().allMatch(Future::isCancelled);
    }

    @Override
    public boolean isDone() {
        return futures.stream().allMatch(Future::isDone);
    }

    @Override
    public Object[] get() {
        return futures.stream().map(f -> {
            try {
                return f.get();
            } catch (InterruptedException | ExecutionException e) {
                return e;
            }
        }).toArray();
    }

    @Override
    public Object[] get(final long timeout, @NotNull final TimeUnit unit) {
        return futures.stream().map(f -> {
            try {
                return f.get(timeout / futures.size(), unit);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                return e;
            }
        }).toArray();
    }

}
