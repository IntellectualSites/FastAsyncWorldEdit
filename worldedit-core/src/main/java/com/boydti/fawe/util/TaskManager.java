package com.boydti.fawe.util;

import static org.slf4j.LoggerFactory.getLogger;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.implementation.queue.QueueHandler;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.RunnableVal;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public abstract class TaskManager {

    public static TaskManager IMP;

    private ForkJoinPool pool = new ForkJoinPool();

    /**
     * Run a repeating task on the main thread
     *
     * @param runnable
     * @param interval in ticks
     * @return
     */
    public abstract int repeat(@NotNull final Runnable runnable, final int interval);

    /**
     * Run a repeating task asynchronously
     *
     * @param runnable
     * @param interval in ticks
     * @return
     */
    public abstract int repeatAsync(@NotNull final Runnable runnable, final int interval);

    /**
     * Run a task asynchronously
     *
     * @param runnable
     */
    public abstract void async(@NotNull final Runnable runnable);

    /**
     * Run a task on the main thread
     *
     * @param runnable
     */
    public abstract void task(@NotNull final Runnable runnable);

    /**
     * Get the public ForkJoinPool<br>
     * - ONLY SUBMIT SHORT LIVED TASKS<br>
     * - DO NOT USE SLEEP/WAIT/LOCKS IN ANY SUBMITTED TASKS<br>
     *
     * @return
     */
    public ForkJoinPool getPublicForkJoinPool() {
        return pool;
    }

    /**
     * Run a bunch of tasks in parallel using the shared thread pool
     *
     * @param runnables
     */
    public void parallel(Collection<Runnable> runnables) {
        for (Runnable run : runnables) {
            pool.submit(run);
        }
        pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    /**
     * Run a bunch of tasks in parallel
     *
     * @param runnables  the tasks to run
     * @param numThreads number of threads (null = config.yml parallel threads)
     */
    @Deprecated
    public void parallel(Collection<Runnable> runnables, @Nullable Integer numThreads) {
        if (runnables == null) {
            return;
        }
        if (numThreads == null) {
            numThreads = Settings.IMP.QUEUE.PARALLEL_THREADS;
        }
        if (numThreads <= 1) {
            for (Runnable run : runnables) {
                if (run != null) {
                    run.run();
                }
            }
            return;
        }
        int numRuns = runnables.size();
        int amountPerThread = 1 + numRuns / numThreads;
        final Runnable[][] split = new Runnable[numThreads][amountPerThread];
        Thread[] threads = new Thread[numThreads];
        int i = 0;
        int j = 0;
        for (Runnable run : runnables) {
            split[i][j] = run;
            if (++i >= numThreads) {
                i = 0;
                j++;
            }
        }
        for (i = 0; i < threads.length; i++) {
            final Runnable[] toRun = split[i];
            Thread thread = threads[i] = new Thread(() -> {
                for (Runnable run : toRun) {
                    if (run != null) {
                        run.run();
                    }
                }
            });
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Disable async catching for a specific task
     *
     * @param run
     */
    public void runUnsafe(Runnable run) {
        QueueHandler queue = Fawe.get().getQueueHandler();
        queue.startSet(true);
        try {
            run.run();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        queue.endSet(true);
    }

    /**
     * Run a task on the current thread or asynchronously
     * - If it's already the main thread, it will jst call run()
     *
     * @param runnable
     * @param async
     */
    public void taskNow(@NotNull final Runnable runnable, boolean async) {
        if (async) {
            async(runnable);
        } else {
            runnable.run();
        }
    }

    /**
     * Run a task as soon as possible on the main thread
     * - Non blocking if not calling from the main thread
     *
     * @param runnable
     */
    public void taskNowMain(@NotNull final Runnable runnable) {
        if (Fawe.isMainThread()) {
            runnable.run();
        } else {
            task(runnable);
        }
    }

    /**
     * Run a task as soon as possible not on the main thread
     *
     * @param runnable
     * @see com.boydti.fawe.Fawe#isMainThread()
     */
    public void taskNowAsync(@NotNull final Runnable runnable) {
        taskNow(runnable, Fawe.isMainThread());
    }

    /**
     * Run a task on the main thread at the next tick or now async
     *
     * @param runnable
     * @param async
     */
    public void taskSoonMain(@NotNull final Runnable runnable, boolean async) {
        if (async) {
            async(runnable);
        } else {
            task(runnable);
        }
    }


    /**
     * Run a task later on the main thread
     *
     * @param runnable
     * @param delay in ticks
     */
    public abstract void later(@NotNull final Runnable runnable, final int delay);

    /**
     * Run a task later asynchronously
     *
     * @param runnable
     * @param delay in ticks
     */
    public abstract void laterAsync(@NotNull final Runnable runnable, final int delay);

    /**
     * Cancel a task
     *
     * @param task
     */
    public abstract void cancel(final int task);

    /**
     * Break up a task and run it in fragments of 5ms.<br>
     * - Each task will run on the main thread.<br>
     *
     * @param objects  the list of objects to run the task for
     * @param task     the task to run on each object
     * @param whenDone when the object task completes
     */
    public <T> void objectTask(Collection<T> objects, final RunnableVal<T> task, final Runnable whenDone) {
        final Iterator<T> iterator = objects.iterator();
        task(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                boolean hasNext;
                while ((hasNext = iterator.hasNext()) && System.currentTimeMillis() - start < 5) {
                    task.value = iterator.next();
                    task.run();
                }
                if (!hasNext) {
                    later(whenDone, 1);
                } else {
                    later(this, 1);
                }
            }
        });
    }

    public void wait(AtomicBoolean running, int timeout) {
        try {
            long start = System.currentTimeMillis();
            synchronized (running) {
                while (running.get()) {
                    running.wait(timeout);
                    if (running.get() && System.currentTimeMillis() - start > Settings.IMP.QUEUE.DISCARD_AFTER_MS) {
                        new RuntimeException("FAWE is taking a long time to execute a task (might just be a symptom): ").printStackTrace();
                        getLogger(TaskManager.class).debug("For full debug information use: /fawe threads");
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void notify(AtomicBoolean running) {
        running.set(false);
        synchronized (running) {
            running.notifyAll();
        }
    }

    public void taskWhenFree(@NotNull Runnable run) {
        if (Fawe.isMainThread()) {
            run.run();
        } else {
            Fawe.get().getQueueHandler().sync(run);
        }
    }

    /**
     * Run a task on the main thread when the TPS is high enough, and wait for execution to finish:<br>
     * - Useful if you need to access something from the Bukkit API from another thread<br>
     * - Usually wait time is around 25ms<br>
     *
     * @param function
     * @return
     */
    public <T> T syncWhenFree(@NotNull final RunnableVal<T> function) {
        if (Fawe.isMainThread()) {
            function.run();
            return function.value;
        }
        try {
            return Fawe.get().getQueueHandler().sync((Supplier<T>) function).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Quickly run a task on the main thread, and wait for execution to finish:<br>
     * - Useful if you need to access something from the Bukkit API from another thread<br>
     * - Usually wait time is around 25ms<br>
     *
     * @param function
     * @return
     */
    public <T> T sync(@NotNull final RunnableVal<T> function) {
        return sync((Supplier<T>) function);
    }

    /**
     * Quickly run a task on the main thread, and wait for execution to finish:<br>
     * - Useful if you need to access something from the Bukkit API from another thread<br>
     * - Usually wait time is around 25ms<br>
     *
     * @param function
     * @return
     */
    public <T> T sync(final Supplier<T> function) {
        if (Fawe.isMainThread()) {
            return function.get();
        }
        try {
            return Fawe.get().getQueueHandler().sync(function).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
