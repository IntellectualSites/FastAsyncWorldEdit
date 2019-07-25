package com.boydti.fawe.util.task;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.QueueHandler;
import com.boydti.fawe.object.Metadatable;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.TaskManager;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class TaskBuilder extends Metadatable {

    private final ForkJoinPool pool = new ForkJoinPool();
    private final ArrayDeque<RunnableTask> tasks;
    private Object result = null;
    private Thread.UncaughtExceptionHandler handler;

    public TaskBuilder() {
        this(null);
    }

    public TaskBuilder(Thread.UncaughtExceptionHandler handler) {
        tasks = new ArrayDeque<>();
        this.handler = handler;
    }

    public TaskBuilder async(Task task) {
        tasks.add(RunnableTask.adapt(task, TaskType.ASYNC));
        return this;
    }

    public TaskBuilder async(ReceiveTask<Object> task) {
        tasks.add(RunnableTask.adapt(task, TaskType.ASYNC));
        return this;
    }

    public TaskBuilder async(ReturnTask task) {
        tasks.add(RunnableTask.adapt(task, TaskType.ASYNC));
        return this;
    }

    public TaskBuilder async(Runnable task) {
        tasks.add(RunnableTask.adapt(task, TaskType.ASYNC));
        return this;
    }

    public TaskBuilder sync(Task task) {
        tasks.add(RunnableTask.adapt(task, TaskType.SYNC));
        return this;
    }

    public TaskBuilder sync(ReceiveTask<Object> task) {
        tasks.add(RunnableTask.adapt(task, TaskType.SYNC));
        return this;
    }

    public TaskBuilder sync(ReturnTask task) {
        tasks.add(RunnableTask.adapt(task, TaskType.SYNC));
        return this;
    }

    public TaskBuilder sync(Runnable task) {
        tasks.add(RunnableTask.adapt(task, TaskType.SYNC));
        return this;
    }

    public TaskBuilder delay(int ticks) {
        tasks.add(RunnableDelayedTask.adapt(ticks));
        return this;
    }

    public TaskBuilder delay(DelayedTask task) {
        tasks.add(RunnableDelayedTask.adapt(task));
        return this;
    }

    /**
     * Run some sync tasks in parallel<br>
     * - All sync parallel tasks which occur directly after each other will be run at the same time
     *
     * @param run
     * @return this
     */
    public TaskBuilder syncParallel(Runnable run) {
        tasks.add(RunnableTask.adapt(run, TaskType.SYNC_PARALLEL));
        return this;
    }

    public TaskBuilder syncParallel(Task run) {
        tasks.add(RunnableTask.adapt(run, TaskType.SYNC_PARALLEL));
        return this;
    }

    public TaskBuilder syncParallel(ReceiveTask<Object> run) {
        tasks.add(RunnableTask.adapt(run, TaskType.SYNC_PARALLEL));
        return this;
    }

    public TaskBuilder syncParallel(ReturnTask run) {
        tasks.add(RunnableTask.adapt(run, TaskType.SYNC_PARALLEL));
        return this;
    }

    /**
     * Run some async tasks in parallel<br>
     * - All async parallel tasks which occur directly after each other will be run at the same time
     *
     * @param run
     * @return this
     */
    public TaskBuilder asyncParallel(Runnable run) {
        tasks.add(RunnableTask.adapt(run, TaskType.ASYNC_PARALLEL));
        return this;
    }

    public TaskBuilder asyncParallel(Task run) {
        tasks.add(RunnableTask.adapt(run, TaskType.ASYNC_PARALLEL));
        return this;
    }

    public TaskBuilder asyncParallel(ReceiveTask<Object> run) {
        tasks.add(RunnableTask.adapt(run, TaskType.ASYNC_PARALLEL));
        return this;
    }

    public TaskBuilder asyncParallel(ReturnTask run) {
        tasks.add(RunnableTask.adapt(run, TaskType.ASYNC_PARALLEL));
        return this;
    }

    /**
     * Run a split task when the server has free time<br>
     * - i.e. To maintain high tps
     * - Use the split() method within task execution
     * - FAWE will be able to pause execution at these points
     *
     * @param run
     * @return this
     */
    public TaskBuilder syncWhenFree(SplitTask run) {
        tasks.add(run);
        return this;
    }

    public TaskBuilder syncWhenFree(Task run) {
        tasks.add(RunnableTask.adapt(run, TaskType.SYNC_WHEN_FREE));
        return this;
    }

    public TaskBuilder syncWhenFree(ReceiveTask<Object> run) {
        tasks.add(RunnableTask.adapt(run, TaskType.SYNC_WHEN_FREE));
        return this;
    }

    public TaskBuilder syncWhenFree(ReturnTask run) {
        tasks.add(RunnableTask.adapt(run, TaskType.SYNC_WHEN_FREE));
        return this;
    }

    public TaskBuilder abortIfTrue(Task<Boolean, Object> run) {
        tasks.add(RunnableTask.adapt(run, TaskType.ABORT));
        return this;
    }

    public TaskBuilder abortIfTrue(final Runnable run) {
        tasks.add(RunnableTask.adapt((Task<Boolean, Boolean>) previous -> {
            if (previous == Boolean.TRUE) run.run();
            return previous == Boolean.TRUE;
        }, TaskType.ABORT));
        return this;
    }

    public TaskBuilder abortIfNull(final Runnable run) {
        tasks.add(RunnableTask.adapt((Task<Boolean, Object>) previous -> {
            if (previous == null) run.run();
            return previous == null;
        }, TaskType.ABORT));
        return this;
    }

    public TaskBuilder abortIfEqual(final Runnable run, final Object other) {
        tasks.add(RunnableTask.adapt((Task<Boolean, Object>) previous -> {
            if (Objects.equals(previous, other)) run.run();
            return Objects.equals(previous, other);
        }, TaskType.ABORT));
        return this;
    }

    public TaskBuilder abortIfNotEqual(final Runnable run, final Object other) {
        tasks.add(RunnableTask.adapt((Task<Boolean, Object>) previous -> {
            if (!Objects.equals(previous, other)) run.run();
            return !Objects.equals(previous, other);
        }, TaskType.ABORT));
        return this;
    }

    /**
     * Have all async tasks run on a new thread<br>
     * - As opposed to trying to using the current thread
     */
    public void buildAsync() {
        TaskManager.IMP.async(this::build);
    }

    /**
     * Begins execution of the tasks<br>
     * - The builder will attempt to run on the current thread if possible
     */
    public void build() {
        RunnableTask peek;
        while ((peek = tasks.peek()) != null) {
            try {
                switch (peek.type) {
                    case DELAY:
                        DelayedTask task = (DelayedTask) tasks.poll();
                        RunnableTask next = tasks.peek();
                        if (next != null) {
                            switch (next.type) {
                                case SYNC:
                                case ABORT:
                                case SYNC_PARALLEL:
                                    TaskManager.IMP.later(this::build, task.getDelay(result));
                                    return;
                                default:
                                    TaskManager.IMP.laterAsync(this::build, task.getDelay(result));
                                    return;
                            }
                        }
                        return;
                    case SYNC:
                    case SYNC_PARALLEL:
                        if (!Fawe.isMainThread()) {
                            TaskManager.IMP.sync(new RunnableVal() {
                                @Override
                                public void run(Object value) {
                                    build();
                                }
                            });
                            return;
                        }
                        break;
                    case SYNC_WHEN_FREE:
                    case ASYNC:
                    case ASYNC_PARALLEL:
                        if (Fawe.isMainThread()) {
                            TaskManager.IMP.async(this::build);
                            return;
                        }
                        break;
                }
                RunnableTask task = tasks.poll();
                task.value = result;
                switch (task.type) {
                    case ABORT:
                        if (((Task<Boolean, Object>) task).run(result)) {
                            return;
                        }
                        break;
                    case SYNC:
                        result = task.exec(result);
                        break;
                    case SYNC_WHEN_FREE:
                        if (task instanceof SplitTask) {
                            SplitTask splitTask = (SplitTask) task;
                            result = splitTask.execSplit(result);
                        } else {
                            result = TaskManager.IMP.syncWhenFree(task);
                        }
                        break;
                    case ASYNC:
                        result = task.exec(result);
                        continue;
                    case SYNC_PARALLEL:
                    case ASYNC_PARALLEL:
                        final ArrayList<RunnableTask> parallel = new ArrayList<>();
                        parallel.add(task);
                        RunnableTask next = tasks.peek();
                        while (next != null && next.type == task.type) {
                            parallel.add(next);
                            tasks.poll();
                            next = tasks.peek();
                        }
                        for (RunnableTask current : parallel) {
                            pool.submit(current);
                        }
                        pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                        result = null;
                        for (RunnableTask current : parallel) {
                            if (current.value != null) {
                                result = current.value;
                            }
                        }
                        break;
                }
                if (task.isAborted()) {
                    return;
                }
            } catch (TaskAbortException abort) {
                return;
            } catch (Throwable e1) {
                if (handler != null) {
                    try {
                        handler.uncaughtException(Thread.currentThread(), e1);
                    } catch (Throwable e2) {
                        e1.printStackTrace();
                        e2.printStackTrace();
                    }
                }
                return;
            }
        }
    }

    public static final class TaskAbortException extends RuntimeException {
        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    private IQueueExtent queue;
    private long last;
    private long start;
    private Object asyncWaitLock = new Object();
    private Object syncWaitLock = new Object();
    private boolean finished;

    private static abstract class RunnableTask<T> extends RunnableVal<T> {
        public final TaskType type;
        private boolean aborted;

        public RunnableTask(TaskType type) {
            this.type = type;
        }

        public void abortNextTasks() {
            this.aborted = true;
        }

        public boolean isAborted() {
            return aborted;
        }

        public static RunnableTask adapt(final Task task, TaskType type) {
            return new RunnableTask(type) {
                @Override
                public Object exec(Object previous) {
                    return task.run(previous);
                }
            };
        }

        public static RunnableTask adapt(final ReturnTask task, TaskType type) {
            return new RunnableTask(type) {
                @Override
                public Object exec(Object previous) {
                    return task.run();
                }
            };
        }

        public static RunnableTask adapt(final ReceiveTask<Object> task, TaskType type) {
            return new RunnableTask(type) {
                @Override
                public Object exec(Object previous) {
                    task.run(previous);
                    return null;
                }
            };
        }

        public static RunnableTask adapt(final Runnable run, TaskType type) {
            return new RunnableTask(type) {
                @Override
                public Object exec(Object previous) {
                    if (run instanceof RunnableVal) {
                        ((RunnableVal) run).value = this.value;
                        return this.value = ((RunnableVal) run).runAndGet();
                    }
                    run.run();
                    return null;
                }
            };
        }

        public abstract T exec(Object previous);

        @Override
        public final void run(T value) {
            this.value = exec(value);
        }
    }

    private static abstract class RunnableDelayedTask extends RunnableTask {

        public RunnableDelayedTask(TaskType type) {
            super(type);
        }

        @Override
        public Object exec(Object previous) {
            return previous;
        }

        public abstract int delay(Object previous);

        public static RunnableDelayedTask adapt(final DelayedTask task) {
            return new RunnableDelayedTask(TaskType.DELAY) {
                @Override
                public int delay(Object previous) {
                    return task.getDelay(previous);
                }
            };
        }

        public static RunnableDelayedTask adapt(final int time) {
            return new RunnableDelayedTask(TaskType.DELAY) {
                @Override
                public int delay(Object previous) {
                    return time;
                }
            };
        }
    }

    public static abstract class SplitTask extends RunnableTask {

        private final long allocation;
        private final QueueHandler queue;
        private long last;
        private long start;
        private Object asyncWaitLock = new Object();
        private Object syncWaitLock = new Object();

        private boolean finished;
        private boolean waitingAsync = true;
        private boolean waitingSync = false;

        public SplitTask() {
            this(20);
        }

        public SplitTask(long allocation) {
            super(TaskType.SYNC_WHEN_FREE);
            this.allocation = allocation;
            this.queue = Fawe.get().getQueueHandler();
        }

        public Object execSplit(final Object previous) {
            this.value = previous;
            final Thread thread = new Thread(() -> {
                try {
                    synchronized (asyncWaitLock) {
                        asyncWaitLock.notifyAll();
                        asyncWaitLock.wait(Long.MAX_VALUE);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                exec(previous);
                finished = true;
                waitingAsync = true;
                waitingSync = false;
                synchronized (syncWaitLock) {
                    syncWaitLock.notifyAll();
                }
            });
            try {
                synchronized (asyncWaitLock) {
                    thread.start();
                    asyncWaitLock.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (thread.isAlive()) {
                TaskManager.IMP.syncWhenFree(new RunnableVal() {
                    @Override
                    public void run(Object ignore) {
                        queue.startSet(true);
                        start = System.currentTimeMillis();
                        try {
                            if (!finished) {
                                synchronized (asyncWaitLock) {
                                    while (!waitingAsync) asyncWaitLock.wait(1);
                                    asyncWaitLock.notifyAll();
                                }
                                waitingSync = true;
                                synchronized (syncWaitLock) {
                                    syncWaitLock.notifyAll();
                                    syncWaitLock.wait();
                                }
                                waitingSync = false;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        queue.endSet(true);
                    }
                });
            }
            return this.value;
        }

        public void split() {
            long now = System.currentTimeMillis();
            if (now - start > allocation) {
                try {
                    synchronized (syncWaitLock) {
                        while (!waitingSync) syncWaitLock.wait(1);
                        syncWaitLock.notifyAll();
                    }
                    waitingAsync = true;
                    synchronized (asyncWaitLock) {
                        asyncWaitLock.notifyAll();
                        asyncWaitLock.wait();
                    }
                    waitingAsync = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private enum TaskType {
        SYNC,
        ASYNC,
        SYNC_PARALLEL,
        ASYNC_PARALLEL,
        SYNC_WHEN_FREE,
        DELAY,
        ABORT
    }
}
