package com.boydti.fawe.object.task;

import com.boydti.fawe.Fawe;
import java.io.Closeable;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class AsyncNotifyQueue implements Closeable {
    private final Lock lock = new ReentrantLock(true);
    private final Thread.UncaughtExceptionHandler handler;
    private boolean closed;

    public AsyncNotifyQueue(Thread.UncaughtExceptionHandler handler) {
        this.handler = handler;
    }

    public Thread.UncaughtExceptionHandler getHandler() {
        return handler;
    }

    public <T> Future<T> run(Runnable task) {
        return call(() -> {
            task.run();
            return null;
        });
    }

    public <T> Future<T> supply(Supplier<T> task) {
        return call(task::get);
    }

    public <T> Future<T> call(Callable<T> task) {
        Future[] self = new Future[1];
        Callable<T> wrapped = () -> {
            if (!closed) {
                lock.lock();
                try {
                    if (!closed) {
                        try {
                            return task.call();
                        } catch (Throwable e) {
                            handler.uncaughtException(Thread.currentThread(), e);
                            if (self[0] != null) self[0].cancel(true);
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
            if (self[0] != null) self[0].cancel(true);
            return null;
        };
        self[0] = Fawe.get().getQueueHandler().async(wrapped);
        return self[0];
    }

    @Override
    public void close() {
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }
}
