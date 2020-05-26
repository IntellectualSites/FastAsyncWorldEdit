package com.boydti.fawe.beta.implementation.queue;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

public class QueuePool<T> extends ConcurrentLinkedQueue<T> implements Pool<T> {
    private final Supplier<T> supplier;

    public QueuePool(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T poll() {
        T result = super.poll();
        if (result == null) {
            return supplier.get();
        }
        return result;
    }

    @Override
    public void clear() {
        if (!isEmpty()) super.clear();
    }
}

