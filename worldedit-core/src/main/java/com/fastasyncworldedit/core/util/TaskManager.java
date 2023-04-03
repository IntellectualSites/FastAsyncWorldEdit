package com.fastasyncworldedit.core.util;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.queue.implementation.QueueHandler;
import com.fastasyncworldedit.core.util.task.RunnableVal;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.util.Location;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public abstract class TaskManager {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    /**
     * @deprecated Use {@link #taskManager()} to get an instance.
     */
    @Deprecated(forRemoval = true, since = "2.0.0")
    public static TaskManager IMP;
    static TaskManager INSTANCE;
    private final ForkJoinPool pool = new ForkJoinPool();

    protected TaskManager() {
        INSTANCE = this;
    }

    /**
     * Gets an instance of the TaskManager.
     *
     * @return an instance of the TaskManager
     * @since 2.0.0
     */
    public static TaskManager taskManager() {
        if (INSTANCE == null) {
            INSTANCE = Fawe.platform().getTaskManager();
        }
        return INSTANCE;
    }

    /**
     * Run a repeating task on the main thread.
     *
     * @param runnable the task to run
     * @param interval in ticks
     */
    @Deprecated
    public abstract int repeat(@Nonnull final Runnable runnable, final int interval);

    /**
     * Run a repeating task asynchronously.
     *
     * @param runnable the task to run
     * @param interval in ticks
     * @return the task id number
     */
    public abstract int repeatAsync(@Nonnull final Runnable runnable, final int interval);

    /**
     * Run a task asynchronously.
     *
     * @param runnable the task to run
     */
    public abstract void async(@Nonnull final Runnable runnable);

    /**
     * Run a task on the main thread.
     *
     * @param runnable the task to run
     */
    @Deprecated
    public abstract void task(@Nonnull final Runnable runnable);
    /**
     * Run a task on the main thread.
     *
     * @param runnable the task to run
     */
    public abstract void task(@Nonnull final Runnable runnable, @Nonnull Location contextLocation);

    /**
     * Get the public ForkJoinPool.
     * - ONLY SUBMIT SHORT LIVED TASKS<br>
     * - DO NOT USE SLEEP/WAIT/LOCKS IN ANY SUBMITTED TASKS<br>
     */
    public ForkJoinPool getPublicForkJoinPool() {
        return pool;
    }

    /**
     * Run a bunch of tasks in parallel using the shared thread pool.
     *
     * @deprecated Deprecated without replacement as unused internally, and poor implementation of what it's designed to do.
     */
    @Deprecated(forRemoval = true, since = "2.7.0")
    public void parallel(Collection<Runnable> runables) {
        for (Runnable run : runables) {
            pool.submit(run);
        }
        pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    /**
     * Run a bunch of tasks in parallel.
     *
     * @param runnables  the tasks to run
     * @param numThreads number of threads (null = config.yml parallel threads)
     * @deprecated Deprecated without replacement as unused internally, and poor implementation of what it's designed to do.
     */
    @Deprecated(forRemoval = true, since = "2.7.0")
    public void parallel(Collection<Runnable> runnables, @Nullable Integer numThreads) {
        if (runnables == null) {
            return;
        }
        if (numThreads == null) {
            numThreads = Settings.settings().QUEUE.PARALLEL_THREADS;
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
     * Disable async catching for a specific task.
     */
    @Deprecated
    public void runUnsafe(Runnable run) {
        QueueHandler queue = Fawe.instance().getQueueHandler();
        queue.startUnsafe(Fawe.isMainThread());
        try {
            run.run();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        queue.endUnsafe(Fawe.isMainThread());
    }

    /**
     * Run a task on the current thread or asynchronously.
     * - If it's already the main thread, it will just call run()
     *
     * @param runnable the task to run
     * @param async    whether the task should run on the main thread
     */
    public void taskNow(@Nonnull final Runnable runnable, boolean async) {
        if (async) {
            async(runnable);
        } else {
            runnable.run();
        }
    }

    /**
     * Run a task as soon as possible on the main thread.
     * - Non blocking if not calling from the main thread
     *
     * @param runnable the task to run
     */
    @Deprecated
    public void taskNowMain(@Nonnull final Runnable runnable) {
        if (Fawe.isTickThread()) {
            runnable.run();
        } else {
            task(runnable);
        }
    }

    /**
     * Run a task as soon as possible not on the main thread.
     *
     * @param runnable the task to run
     * @see Fawe#isTickThread()
     */
    public void taskNowAsync(@Nonnull final Runnable runnable) {
        taskNow(runnable, Fawe.isTickThread());
    }

    /**
     * Run a task on the main thread at the next tick or now async.
     *
     * @param runnable the task to run.
     * @param async    whether the task should run on the main thread
     */
    @Deprecated
    public void taskSoonMain(@Nonnull final Runnable runnable, boolean async) {
        if (async) {
            async(runnable);
        } else {
            task(runnable);
        }
    }


    /**
     * Run a task later on the main thread.
     *
     * @param runnable the task to run
     * @param delay    in ticks
     */
    @Deprecated
    public abstract void later(@Nonnull final Runnable runnable, final int delay);
    public abstract void later(@Nonnull final Runnable runnable, Location location, final int delay);

    /**
     * Run a task later asynchronously.
     *
     * @param runnable the task to run
     * @param delay    in ticks
     */
    public abstract void laterAsync(@Nonnull final Runnable runnable, final int delay);

    /**
     * Cancel a task.
     *
     * @param task the id of the task to cancel
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
    @Deprecated
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

    /**
     * @deprecated Deprecated without replacement as unused internally, and poor implementation of what it's designed to do.
     */
    @Deprecated(forRemoval = true, since = "2.7.0")
    public void wait(AtomicBoolean running, int timeout) {
        try {
            long start = System.currentTimeMillis();
            synchronized (running) {
                while (running.get()) {
                    running.wait(timeout);
                    if (running.get() && System.currentTimeMillis() - start > 60000) {
                        new RuntimeException("FAWE is taking a long time to execute a task (might just be a symptom): ").printStackTrace();
                        LOGGER.info("For full debug information use: /fawe threads");
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @deprecated Deprecated without replacement as unused internally, and poor implementation of what it's designed to do.
     */
    @Deprecated(forRemoval = true, since = "2.7.0")
    public void notify(AtomicBoolean running) {
        running.set(false);
        synchronized (running) {
            running.notifyAll();
        }
    }

    @Deprecated
    public void taskWhenFree(@Nonnull Runnable run) {
        if (Fawe.isTickThread()) {
            run.run();
        } else {
            Fawe.instance().getQueueHandler().sync(run);
        }
    }

    /**
     * Run a task on the main thread when the TPS is high enough, and wait for execution to finish.
     * - Useful if you need to access something from the Bukkit API from another thread<br>
     * - Usually wait time is around 25ms<br>
     */
    @Deprecated
    public <T> T syncWhenFree(@Nonnull final RunnableVal<T> function) {
        if (Fawe.isTickThread()) {
            function.run();
            return function.value;
        }
        try {
            return Fawe.instance().getQueueHandler().sync((Supplier<T>) function).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Run a task on the main thread when the TPS is high enough, and wait for execution to finish.
     * - Useful if you need to access something from the Bukkit API from another thread<br>
     * - Usually wait time is around 25ms<br>
     */
    @Deprecated
    public <T> T syncWhenFree(@Nonnull final Supplier<T> supplier) {
        if (Fawe.isTickThread()) {
            return supplier.get();
        }
        try {
            return Fawe.instance().getQueueHandler().sync(supplier).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Quickly run a task on the main thread, and wait for execution to finish.
     * - Useful if you need to access something from the Bukkit API from another thread<br>
     * - Usually wait time is around 25ms
     */
    @Deprecated
    public <T> T sync(@Nonnull final RunnableVal<T> function) {
        return sync((Supplier<T>) function);
    }

    /**
     * Quickly run a task on the main thread, and wait for execution to finish.
     * - Useful if you need to access something from the Bukkit API from another thread<br>
     * - Usually wait time is around 25ms<br>
     */
    @Deprecated
    public <T> T sync(final Supplier<T> function) {
        if (Fawe.isTickThread()) {
            return function.get();
        }
        try {
            return Fawe.instance().getQueueHandler().sync(function).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract <T> T syncAt(Supplier<T> supplier, Location context);

    public abstract <T> T syncWith(Supplier<T> supplier, Player context);

}
