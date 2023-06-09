package com.fastasyncworldedit.core.util.task;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Executor service that queues tasks based on keys, executing tasks on a configurable {@link ThreadPoolExecutor}
 *
 * @param <K> Key type
 * @since 2.6.2
 */
public class KeyQueuedExecutorService<K> {

    private final ExecutorService parent;
    private final Map<K, KeyRunner> keyQueue = new HashMap<>();

    /**
     * Create a new {@link KeyQueuedExecutorService} instance
     *
     * @param parent Parent {@link ExecutorService} to use for actual task completion
     */
    public KeyQueuedExecutorService(ExecutorService parent) {
        this.parent = parent;
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
        synchronized (keyQueue) {
            boolean triggerRun = false;
            KeyRunner runner = keyQueue.get(key);
            if (runner == null) {
                runner = new KeyRunner(key);
                keyQueue.put(key, runner);
                triggerRun = true;
            }
            runner.add(command);
            if (triggerRun) {
                runner.triggerRun();
            }
        }
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

        void triggerRun() {
            Runnable task = tasks.poll();
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
                Runnable next = tasks.poll();
                if (next == null) {
                    synchronized (keyQueue) {
                        next = tasks.poll();
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
