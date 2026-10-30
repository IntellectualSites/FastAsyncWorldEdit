package com.fastasyncworldedit.core.util.task;

import com.fastasyncworldedit.core.configuration.Settings;
import com.sk89q.worldedit.session.request.Request;

import java.io.Closeable;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * async queue that accepts a {@link Thread.UncaughtExceptionHandler} for exception handling per instance, delegating to a
 * parent {@link KeyQueuedExecutorService}.
 *
 * @since 2.7.0
 */
public class AsyncNotifyKeyedQueue implements Closeable {

    private static final KeyQueuedExecutorService<UUID> QUEUE_SUBMISSIONS = new KeyQueuedExecutorService<>(new ForkJoinPool(
            Settings.settings().QUEUE.PARALLEL_THREADS,
            new FaweForkJoinWorkerThreadFactory("AsyncNotifyKeyedQueue - %s"),
            null,
            false
    ));

    private final Thread.UncaughtExceptionHandler handler;
    private final Supplier<UUID> key;
    private volatile boolean closed;

    /**
     * New instance
     *
     * @param handler exception handler
     * @param key     supplier of UUID key
     */
    public AsyncNotifyKeyedQueue(Thread.UncaughtExceptionHandler handler, Supplier<UUID> key) {
        this.handler = handler;
        this.key = key;
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

    public <T> Future<T> call(Callable<T> task) {
        Future[] self = new Future[1];
        Callable<T> wrapped = () -> {
            if (!closed) {
                // Pool threads are reused, so a request set by this task would otherwise keep its edit session (and the
                // actor) alive until the thread runs another task. Only clear it if this task created it, so a task
                // running nested inside another task on the same thread does not remove the outer task's request.
                boolean ownsRequest = !Request.hasRequest();
                try {
                    return task.call();
                } catch (Throwable e) {
                    handler.uncaughtException(Thread.currentThread(), e);
                } finally {
                    if (ownsRequest) {
                        Request.reset();
                    }
                }
            }
            if (self[0] != null) {
                self[0].cancel(true);
            }
            return null;
        };
        self[0] = QUEUE_SUBMISSIONS.submit(key.get(), wrapped);
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
