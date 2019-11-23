package com.boydti.fawe.object.collection;

import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.Iterator;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class YieldIterable<T> implements Iterable<T>, Consumer<T>, Closeable {
    private static final Object END_MARKER = new Object();
    private final LinkedBlockingQueue<T> queue;
    private Future future;

    public YieldIterable(@Nullable Future task) {
        this.queue = new LinkedBlockingQueue<>();
        this.future = task;
    }

    public YieldIterable() {
        this(null);
    }

    public void setFuture(Future future) {
        this.future = future;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private boolean interrupted;
            private T buffer;

            @Override
            public boolean hasNext() {
                try {
                    while (buffer == null && !interrupted && (future == null || !future.isCancelled())) {
                        buffer = queue.poll(50, TimeUnit.MILLISECONDS);
                        if (buffer == END_MARKER) {
                            interrupted = true;
                            return false;
                        }
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                }
                return buffer != null;
            }

            @Override
            public T next() {
                hasNext();
                T result = buffer;
                buffer = null;
                return result;
            }
        };
    }

    @Override
    public void accept(T t) {
        queue.add(t);
    }

    @Override
    public void close() {
        queue.add((T) END_MARKER);
    }
}
