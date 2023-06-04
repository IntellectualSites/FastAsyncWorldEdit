package com.fastasyncworldedit.core.util.task;

import com.fastasyncworldedit.core.configuration.Settings;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Executor service that queues tasks based on keys, executing tasks on a configurable {@link ThreadPoolExecutor}
 *
 * @param <K> Key type
 */
public class KeyQueuedExecutorService<K> {

    private final ExecutorService parent;
    private final Map<K, KeyRunner> keyQueue = new HashMap<>();

    /**
     * Create a new {@link KeyQueuedExecutorService} with default settings:
     *  - corePoolSize    = 1
     *  - maximumPoolSize = Settings.settings().QUEUE.PARALLEL_THREADS
     *  - keepAliveTime   = 0
     */
    public KeyQueuedExecutorService() {
        this(1, Settings.settings().QUEUE.PARALLEL_THREADS, 0L, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a new {@code KeyQueuedExecutorService} with the given initial
     * parameters, the
     * {@linkplain Executors#defaultThreadFactory default thread factory}
     * and the {@linkplain ThreadPoolExecutor.AbortPolicy
     * default rejected execution handler}.
     *
     * <p>It may be more convenient to use one of the {@link Executors}
     * factory methods instead of this general purpose constructor.
     *
     * @param corePoolSize    the number of threads to keep in the pool, even
     *                        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *                        pool
     * @param keepAliveTime   when the number of threads is greater than
     *                        the core, this is the maximum time that excess idle threads
     *                        will wait for new tasks before terminating.
     * @param unit            the time unit for the {@code keepAliveTime} argument
     * @throws IllegalArgumentException if one of the following holds:<br>
     *                                  {@code corePoolSize < 0}<br>
     *                                  {@code keepAliveTime < 0}<br>
     *                                  {@code maximumPoolSize <= 0}<br>
     *                                  {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException     if {@code workQueue} is null
     */
    public KeyQueuedExecutorService(
            int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit
    ) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, Executors.defaultThreadFactory());
    }

    /**
     * Creates a new {@code KeyQueuedExecutorService} with the given initial
     * parameters and the {@linkplain ThreadPoolExecutor.AbortPolicy
     * default rejected execution handler}.
     *
     * @param corePoolSize    the number of threads to keep in the pool, even
     *                        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *                        pool
     * @param keepAliveTime   when the number of threads is greater than
     *                        the core, this is the maximum time that excess idle threads
     *                        will wait for new tasks before terminating.
     * @param unit            the time unit for the {@code keepAliveTime} argument
     * @param threadFactory   the factory to use when the executor
     *                        creates a new thread
     * @throws IllegalArgumentException if one of the following holds:<br>
     *                                  {@code corePoolSize < 0}<br>
     *                                  {@code keepAliveTime < 0}<br>
     *                                  {@code maximumPoolSize <= 0}<br>
     *                                  {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException     if {@code workQueue}
     *                                  or {@code threadFactory} is null
     */
    public KeyQueuedExecutorService(
            int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, ThreadFactory threadFactory
    ) {
        parent = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                new LinkedBlockingQueue<>(),
                threadFactory
        );
    }

    /**
     * Delegates to {@link ThreadPoolExecutor#shutdown()}
     */
    public void shutdown() {
        parent.shutdown();
    }

    /**
     * Delegates to {@link ThreadPoolExecutor#shutdownNow()}
     */
    @Nonnull
    public List<Runnable> shutdownNow() {
        return parent.shutdownNow();
    }

    /**
     * Delegates to {@link ThreadPoolExecutor#isShutdown()}
     */
    public boolean isShutdown() {
        return parent.isShutdown();
    }

    /**
     * Delegates to {@link ThreadPoolExecutor#isTerminated()}
     */
    public boolean isTerminated() {
        return parent.isTerminated();
    }

    /**
     * Delegates to {@link ThreadPoolExecutor#awaitTermination(long, TimeUnit)}
     */
    public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        return parent.awaitTermination(timeout, unit);
    }

    protected <T> FutureTask<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask<>(runnable, value);
    }

    protected <T> FutureTask<T> newTaskFor(Callable<T> callable) {
        return new FutureTask<>(callable);
    }

    @Nonnull
    public <T> Future<T> submit(@Nonnull K key, @Nonnull Callable<T> task) {
        FutureTask<T> ftask = newTaskFor(task);
        execute(key, ftask);
        return ftask;
    }

    @Nonnull
    public <T> Future<T> submit(@Nonnull K key, @Nonnull Runnable task, T result) {
        FutureTask<T> ftask = newTaskFor(task, result);
        execute(key, ftask);
        return ftask;
    }

    @Nonnull
    public Future<?> submit(@Nonnull K key, @Nonnull Runnable task) {
        FutureTask<Void> ftask = newTaskFor(task, null);
        execute(key, ftask);
        return ftask;
    }

    public void execute(@Nonnull K key, @Nonnull FutureTask<?> command) {
        KeyRunner runner;
        synchronized (keyQueue) {
            runner = keyQueue.merge(key, new KeyRunner(key), (existing, theNew) -> {
                existing.add(command);
                return existing;
            });
        }
        runner.triggerRun();
    }

    private final class KeyRunner {

        private final Queue<FutureTask<?>> tasks = new ConcurrentLinkedQueue<>();
        private final K key;

        private KeyRunner(K key) {
            this.key = key;
        }

        void add(FutureTask<?> task) {
            if (!tasks.add(task)) {
                throw new RejectedExecutionException(rejection());
            }
        }

        synchronized void triggerRun() {
            Runnable task = tasks.remove();
            if (task == null) {
                throw new RejectedExecutionException(rejection());
            }
            try {
                run(task);
            } catch (RejectedExecutionException e) {
                synchronized (keyQueue) {
                    keyQueue.remove(key);
                }
                throw new RejectedExecutionException(rejection(), e);
            }
        }

        private void run(Runnable task) {
            parent.execute(() -> {
                task.run();
                Runnable next = tasks.remove();
                if (next == null) {
                    synchronized (keyQueue) {
                        next = tasks.remove();
                        if (next == null) {
                            keyQueue.remove(key);
                        }
                    }
                }
                if (next != null) {
                    run(next);
                }
            });
        }

        private String rejection() {
            return "Task for the key '" + key + "' rejected";
        }

    }

}
