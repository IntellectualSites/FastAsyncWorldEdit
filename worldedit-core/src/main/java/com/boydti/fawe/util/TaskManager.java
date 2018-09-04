package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RunnableVal;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.annotation.Nullable;

public abstract class TaskManager {

    public static TaskManager IMP;

    private ForkJoinPool pool = new ForkJoinPool();

    /**
     * Run a repeating task on the main thread
     *
     * @param r
     * @param interval in ticks
     * @return
     */
    public abstract int repeat(final Runnable r, final int interval);

    /**
     * Run a repeating task asynchronously
     *
     * @param r
     * @param interval in ticks
     * @return
     */
    public abstract int repeatAsync(final Runnable r, final int interval);

    /**
     * Run a task asynchronously
     *
     * @param r
     */
    public abstract void async(final Runnable r);

    /**
     * Run a task on the main thread
     *
     * @param r
     */
    public abstract void task(final Runnable r);

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
     * Run a buch of tasks in parallel using the shared thread pool
     *
     * @param runnables
     */
    public void parallel(Collection<Runnable> runnables) {
//        if (!Fawe.get().isJava8()) {
//            ExecutorCompletionService c = new ExecutorCompletionService(pool);
//            for (Runnable run : runnables) {
//                c.submit(run, null);
//            }
//            try {
//                for (int i = 0; i < runnables.size(); i++) {
//                    c.take();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return;
//        }
        for (Runnable run : runnables) {
            pool.submit(run);
        }
        pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    /**
     * Run a bunch of tasks in parallel
     *
     * @param runnables  The tasks to run
     * @param numThreads Number of threads (null = config.yml parallel threads)
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
            Thread thread = threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < toRun.length; j++) {
                        Runnable run = toRun[j];
                        if (run != null) {
                            run.run();
                        }
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
     * @param queue
     * @param run
     */
    public void runUnsafe(FaweQueue queue, Runnable run) {
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
     * @param r
     * @param async
     */
    public void taskNow(final Runnable r, boolean async) {
        if (async) {
            async(r);
        } else if (r != null) {
            r.run();
        }
    }

    /**
     * Run a task as soon as possible on the main thread
     * - Non blocking if not calling from the main thread
     *
     * @param r
     */
    public void taskNowMain(final Runnable r) {
        if (r == null) {
            return;
        }
        if (Fawe.isMainThread()) {
            r.run();
        } else {
            task(r);
        }
    }

    /**
     * Run a task as soon as possible not on the main thread
     *
     * @param r
     * @see com.boydti.fawe.Fawe#isMainThread()
     */
    public void taskNowAsync(final Runnable r) {
        taskNow(r, Fawe.isMainThread());
    }

    /**
     * Run a task on the main thread at the next tick or now async
     *
     * @param r
     * @param async
     */
    public void taskSoonMain(final Runnable r, boolean async) {
        if (async) {
            async(r);
        } else {
            task(r);
        }
    }


    /**
     * Run a task later on the main thread
     *
     * @param r
     * @param delay in ticks
     */
    public abstract void later(final Runnable r, final int delay);

    /**
     * Run a task later asynchronously
     *
     * @param r
     * @param delay in ticks
     */
    public abstract void laterAsync(final Runnable r, final int delay);

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
     * @param objects  - The list of objects to run the task for
     * @param task     - The task to run on each object
     * @param whenDone - When the object task completes
     * @param <T>
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

    /**
     * Quickly run a task on the main thread, and wait for execution to finish:<br>
     * - Useful if you need to access something from the Bukkit API from another thread<br>
     * - Usualy wait time is around 25ms<br>
     *
     * @param function
     * @param <T>
     * @return
     */
    public <T> T sync(final RunnableVal<T> function) {
        return sync(function, Integer.MAX_VALUE);
    }

    public <T> T sync(final Supplier<T> function) {
        return sync(function, Integer.MAX_VALUE);
    }

    public void wait(AtomicBoolean running, int timout) {
        try {
            long start = System.currentTimeMillis();
            synchronized (running) {
                while (running.get()) {
                    running.wait(timout);
                    if (running.get() && System.currentTimeMillis() - start > Settings.IMP.QUEUE.DISCARD_AFTER_MS) {
                        new RuntimeException("FAWE is taking a long time to execute a task (might just be a symptom): ").printStackTrace();
                        Fawe.debug("For full debug information use: /fawe threads");
                    }
                }
            }
        } catch (InterruptedException e) {
            MainUtil.handleError(e);
        }
    }

    public void notify(AtomicBoolean running) {
        running.set(false);
        synchronized (running) {
            running.notifyAll();
        }
    }

    public <T> T syncWhenFree(final RunnableVal<T> function) {
        return syncWhenFree(function, Integer.MAX_VALUE);
    }

    public void taskWhenFree(Runnable run) {
        if (Fawe.isMainThread()) {
            run.run();
        } else {
            SetQueue.IMP.addTask(run);
        }
    }

    /**
     * Run a task on the main thread when the TPS is high enough, and wait for execution to finish:<br>
     * - Useful if you need to access something from the Bukkit API from another thread<br>
     * - Usualy wait time is around 25ms<br>
     *
     * @param function
     * @param timeout  - How long to wait for execution
     * @param <T>
     * @return
     */
    public <T> T syncWhenFree(final RunnableVal<T> function, int timeout) {
        if (Fawe.isMainThread()) {
            function.run();
            return function.value;
        }
        final AtomicBoolean running = new AtomicBoolean(true);
        RunnableVal<RuntimeException> run = new RunnableVal<RuntimeException>() {
            @Override
            public void run(RuntimeException value) {
                try {
                    function.run();
                } catch (RuntimeException e) {
                    this.value = e;
                } catch (Throwable neverHappens) {
                    MainUtil.handleError(neverHappens);
                } finally {
                    running.set(false);
                }
                synchronized (function) {
                    function.notifyAll();
                }
            }
        };
        SetQueue.IMP.addTask(run);
        try {
            synchronized (function) {
                while (running.get()) {
                    function.wait(timeout);
                }
            }
        } catch (InterruptedException e) {
            MainUtil.handleError(e);
        }
        if (run.value != null) {
            throw run.value;
        }
        return function.value;
    }

    /**
     * Quickly run a task on the main thread, and wait for execution to finish:<br>
     * - Useful if you need to access something from the Bukkit API from another thread<br>
     * - Usualy wait time is around 25ms<br>
     *
     * @param function
     * @param timeout  - How long to wait for execution
     * @param <T>
     * @return
     */
    public <T> T sync(final RunnableVal<T> function, int timeout) {
        return sync((Supplier<T>) function, timeout);
    }

    public <T> T sync(final Supplier<T> function, int timeout) {
        if (Fawe.isMainThread()) {
            return function.get();
        }
        final AtomicBoolean running = new AtomicBoolean(true);
        RunnableVal<Object> run = new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                try {
                    this.value = function.get();
                } catch (RuntimeException e) {
                    this.value = e;
                } catch (Throwable neverHappens) {
                    MainUtil.handleError(neverHappens);
                } finally {
                    running.set(false);
                    synchronized (function) {
                        function.notifyAll();
                    }
                }
            }
        };
        SetQueue.IMP.addTask(run);
        try {
            synchronized (function) {
                while (running.get()) {
                    function.wait(timeout);
                }
            }
        } catch (InterruptedException e) {
            MainUtil.handleError(e);
        }
        if (run.value != null && run.value instanceof RuntimeException) {
            throw (RuntimeException) run.value;
        }
        return (T) run.value;
    }
}
