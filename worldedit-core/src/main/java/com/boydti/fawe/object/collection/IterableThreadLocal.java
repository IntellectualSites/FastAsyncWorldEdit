package com.boydti.fawe.object.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;

public class IterableThreadLocal<T> extends ThreadLocal<T> implements Iterable<T> {
    private final ConcurrentLinkedDeque<T> allValues = new ConcurrentLinkedDeque<>();
    private final Supplier<T> supplier;

    public IterableThreadLocal(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    protected final T initialValue() {
        T value = init();
        if (value != null) {
            synchronized (this) {
                allValues.add(value);
            }
        }
        return value;
    }

    @Override
    public final Iterator<T> iterator() {
        return getAll().iterator();
    }

    public T init() {
        return supplier.get();
    }

    public void clean() {
        if (!allValues.isEmpty()) {
            synchronized (this) {
                CleanableThreadLocal.clean(this);
                allValues.clear();
            }
        }
    }
    public final Collection<T> getAll() {
        return Collections.unmodifiableCollection(allValues);
    }

    @Override
    protected void finalize() throws Throwable {
        CleanableThreadLocal.clean(this);
        super.finalize();
    }
}
