package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.sk89q.worldedit.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class SetQueue {

    /**
     * The implementation specific queue
     */
    public static final SetQueue IMP = new SetQueue();
    private double targetTPS = 18;

    public enum QueueStage {
        INACTIVE, ACTIVE, NONE;
    }

    private final ConcurrentLinkedDeque<FaweQueue> activeQueues;
    private final ConcurrentLinkedDeque<FaweQueue> inactiveQueues;
    private final ConcurrentLinkedDeque<Runnable> tasks;

    /**
     * Used to calculate elapsed time in milliseconds and ensure block placement doesn't lag the server
     */
    private long last;
    private long allocate = 50;
    private long lastSuccess;

    /**
     * A queue of tasks that will run when the queue is empty
     */
    private final ConcurrentLinkedDeque<Runnable> emptyTasks = new ConcurrentLinkedDeque<>();

    private ForkJoinPool pool = new ForkJoinPool();
    private ExecutorCompletionService completer = new ExecutorCompletionService(pool);

    /**
     * @return ForkJoinPool
     * @see TaskManager#getPublicForkJoinPool()
     */
    @Deprecated
    public ExecutorCompletionService getCompleterService() {
        return completer;
    }

    @Deprecated
    public ForkJoinPool getForkJoinPool() {
        return pool;
    }

    public void runMiscTasks() {
        while (Fawe.get().getTimer().isAbove(targetTPS)) {
            Runnable task = tasks.poll();
            if (task != null) {
                task.run();
            } else {
                break;
            }
        }
    }

    public SetQueue() {
        tasks = new ConcurrentLinkedDeque<>();
        activeQueues = new ConcurrentLinkedDeque<>();
        inactiveQueues = new ConcurrentLinkedDeque<>();
        if (TaskManager.IMP == null) return;
        TaskManager.IMP.repeat(() -> {
            try {
                long now = System.currentTimeMillis();
                boolean empty = (inactiveQueues.isEmpty() && activeQueues.isEmpty());
                boolean emptyTasks = tasks.isEmpty();
                if (emptyTasks && empty) {
                    last = now;
                    runEmptyTasks();
                    return;
                }

                targetTPS = 18 - Math.max(Settings.IMP.QUEUE.EXTRA_TIME_MS * 0.05, 0);

                long diff = (50 + SetQueue.this.last) - (SetQueue.this.last = now);
                long absDiff = Math.abs(diff);
                if (diff == 0) {
                    allocate = Math.min(50, allocate + 1);
                } else if (diff < 0) {
                    allocate = Math.max(5, allocate + diff);
                } else if (!Fawe.get().getTimer().isAbove(targetTPS)) {
                    allocate = Math.max(5, allocate - 1);
                }

                long currentAllocate = allocate - absDiff;

                if (!emptyTasks) {
                    long taskAllocate = activeQueues.isEmpty() ? currentAllocate : 1 + (currentAllocate >> 1);
                    long used = 0;
                    boolean wait = false;
                    do {
                        Runnable task = tasks.poll();
                        if (task == null) {
                            if (wait) {
                                synchronized (tasks) {
                                    tasks.wait(1);
                                }
                                task = tasks.poll();
                                wait = false;
                            } else {
                                break;
                            }
                        }
                        if (task != null) {
                            task.run();
                            wait = true;
                        }
                    } while ((used = System.currentTimeMillis() - now) < taskAllocate);
                    currentAllocate -= used;
                }

                if (empty) {
                    runEmptyTasks();
                    return;
                }

                if (!MemUtil.isMemoryFree()) {
                    final int mem = MemUtil.calculateMemory();
                    if (mem != Integer.MAX_VALUE) {
                        allocate = Math.max(5, allocate - 1);
                        if ((mem <= 1) && Settings.IMP.PREVENT_CRASHES) {
                            for (FaweQueue queue : getAllQueues()) {
                                queue.saveMemory();
                            }
                            return;
                        }
                        if (SetQueue.this.forceChunkSet()) {
                            System.gc();
                        } else {
                            SetQueue.this.runEmptyTasks();
                        }
                        return;
                    }
                }

                FaweQueue queue = getNextQueue();
                if (queue == null) {
                    return;
                }

                long time = (long) Settings.IMP.QUEUE.EXTRA_TIME_MS + currentAllocate - System.currentTimeMillis() + now;
                // Disable the async catcher as it can't discern async vs parallel
                boolean parallel = Settings.IMP.QUEUE.PARALLEL_THREADS > 1;
                queue.startSet(parallel);
                try {
                    if (!queue.next(Settings.IMP.QUEUE.PARALLEL_THREADS, time) && queue.getStage() == QueueStage.ACTIVE) {
                        queue.setStage(QueueStage.NONE);
                        queue.runTasks();
                    }
                } catch (Throwable e) {
                    pool.awaitQuiescence(Settings.IMP.QUEUE.DISCARD_AFTER_MS, TimeUnit.MILLISECONDS);
                    completer = new ExecutorCompletionService(pool);
                    e.printStackTrace();
                }
                if (pool.getQueuedSubmissionCount() != 0 || pool.getRunningThreadCount() != 0 || pool.getQueuedTaskCount() != 0) {
//                        if (Fawe.get().isJava8())
                    {
                        pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                    }
//                        else {
//                            pool.shutdown();
//                            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
//                            pool = new ForkJoinPool();
//                            completer = new ExecutorCompletionService(pool);
//                        }
                }
                queue.endSet(parallel);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }, 1);
    }

    public QueueStage getStage(FaweQueue queue) {
        return queue.getStage();
    }

    public boolean isStage(FaweQueue queue, QueueStage stage) {
        switch (stage) {
            case ACTIVE:
                return activeQueues.contains(queue);
            case INACTIVE:
                return inactiveQueues.contains(queue);
            case NONE:
                return !activeQueues.contains(queue) && !inactiveQueues.contains(queue);
        }
        return false;
    }

    public boolean enqueue(FaweQueue queue) {
        queue.setStage(QueueStage.ACTIVE);
        inactiveQueues.remove(queue);
        if (queue.size() > 0) {
            if (!activeQueues.contains(queue)) {
                queue.optimize();
                activeQueues.add(queue);
            }
            return true;
        }
        return false;
    }

    public void dequeue(FaweQueue queue) {
        queue.setStage(QueueStage.NONE);
        inactiveQueues.remove(queue);
        activeQueues.remove(queue);
        queue.runTasks();
    }

    public Collection<FaweQueue> getAllQueues() {
        ArrayList<FaweQueue> list = new ArrayList<>(activeQueues.size() + inactiveQueues.size());
        list.addAll(inactiveQueues);
        list.addAll(activeQueues);
        return list;
    }

    public Collection<FaweQueue> getActiveQueues() {
        return Collections.unmodifiableCollection(activeQueues);
    }

    public Collection<FaweQueue> getInactiveQueues() {
        return Collections.unmodifiableCollection(inactiveQueues);
    }

    public FaweQueue getNewQueue(World world, boolean fast, boolean autoqueue) {
        world = WorldWrapper.unwrap(world);
        if (world instanceof FaweQueue) return (FaweQueue) world;
        FaweQueue queue = Fawe.imp().getNewQueue(world, fast);
        if (autoqueue) {
            queue.setStage(QueueStage.INACTIVE);
            inactiveQueues.add(queue);
        }
        return queue;
    }

    public FaweQueue getNewQueue(String world, boolean fast, boolean autoqueue) {
        FaweQueue queue = Fawe.imp().getNewQueue(world, fast);
        if (autoqueue) {
            queue.setStage(QueueStage.INACTIVE);
            inactiveQueues.add(queue);
        }
        return queue;
    }

    public void flush(FaweQueue queue) {
        int parallelThreads;
        if (Fawe.isMainThread()) {
            parallelThreads = Settings.IMP.QUEUE.PARALLEL_THREADS;
            Settings.IMP.QUEUE.PARALLEL_THREADS = 1;
        } else {
            parallelThreads = 0;
        }
        try {
            queue.startSet(Settings.IMP.QUEUE.PARALLEL_THREADS > 1);
            queue.next(Settings.IMP.QUEUE.PARALLEL_THREADS, Long.MAX_VALUE);
        } catch (Throwable e) {
            pool.awaitQuiescence(Settings.IMP.QUEUE.DISCARD_AFTER_MS, TimeUnit.MILLISECONDS);
            completer = new ExecutorCompletionService(pool);
            e.printStackTrace();
        } finally {
            queue.endSet(Settings.IMP.QUEUE.PARALLEL_THREADS > 1);
            queue.setStage(QueueStage.NONE);
            queue.runTasks();
            if (parallelThreads != 0) {
                Settings.IMP.QUEUE.PARALLEL_THREADS = parallelThreads;
            }
        }
    }

    public FaweQueue getNextQueue() {
        long now = System.currentTimeMillis();
        while (!activeQueues.isEmpty()) {
            FaweQueue queue = activeQueues.peek();
            if (queue != null && queue.size() > 0) {
                queue.setModified(now);
                return queue;
            } else {
                queue.setStage(QueueStage.NONE);
                queue.runTasks();
                activeQueues.poll();
            }
        }
        int size = inactiveQueues.size();
        if (size > 0) {
            Iterator<FaweQueue> iter = inactiveQueues.iterator();
            try {
                int total = 0;
                FaweQueue firstNonEmpty = null;
                while (iter.hasNext()) {
                    FaweQueue queue = iter.next();
                    long age = now - queue.getModified();
                    total += queue.size();
                    if (queue.size() == 0) {
                        if (age > Settings.IMP.QUEUE.DISCARD_AFTER_MS) {
                            queue.setStage(QueueStage.NONE);
                            queue.runTasks();
                            iter.remove();
                        }
                        continue;
                    }
                    if (firstNonEmpty == null) {
                        firstNonEmpty = queue;
                    }
                    if (total > Settings.IMP.QUEUE.TARGET_SIZE) {
                        firstNonEmpty.setModified(now);
                        return firstNonEmpty;
                    }
                    if (age > Settings.IMP.QUEUE.MAX_WAIT_MS) {
                        queue.setModified(now);
                        return queue;
                    }
                }
            } catch (ConcurrentModificationException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean next() {
        while (activeQueues.size() > 0) {
            FaweQueue queue = activeQueues.poll();
            if (queue != null) {
                final boolean set = queue.next();
                if (set) {
                    activeQueues.add(queue);
                    return set;
                } else {
                    queue.setStage(QueueStage.NONE);
                    queue.runTasks();
                }
            }
        }
        if (inactiveQueues.size() > 0) {
            ArrayList<FaweQueue> tmp = new ArrayList<>(inactiveQueues);
            if (Settings.IMP.QUEUE.MAX_WAIT_MS != -1) {
                long now = System.currentTimeMillis();
                if (lastSuccess == 0) {
                    lastSuccess = now;
                }
                long diff = now - lastSuccess;
                if (diff > Settings.IMP.QUEUE.MAX_WAIT_MS) {
                    for (FaweQueue queue : tmp) {
                        boolean result = queue.next();
                        if (result) {
                            return result;
                        }
                    }
                    if (diff > Settings.IMP.QUEUE.DISCARD_AFTER_MS) {
                        // These edits never finished
                        for (FaweQueue queue : tmp) {
                            queue.setStage(QueueStage.NONE);
                            queue.runTasks();
                        }
                        inactiveQueues.clear();
                    }
                    return false;
                }
            }
            if (Settings.IMP.QUEUE.TARGET_SIZE != -1) {
                int total = 0;
                for (FaweQueue queue : tmp) {
                    total += queue.size();
                }
                if (total > Settings.IMP.QUEUE.TARGET_SIZE) {
                    for (FaweQueue queue : tmp) {
                        boolean result = queue.next();
                        if (result) {
                            return result;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean forceChunkSet() {
        return next();
    }

    /**
     * Is the this empty
     *
     * @return
     */
    public boolean isEmpty() {
        return activeQueues.size() == 0 && inactiveQueues.size() == 0;
    }

    public void addTask(Runnable whenFree) {
        tasks.add(whenFree);
        synchronized (tasks) {
            tasks.notifyAll();
        }
    }

    /**
     * Add a task to run when it is empty
     *
     * @param whenDone
     * @return
     */
    public boolean addEmptyTask(final Runnable whenDone) {
        if (this.isEmpty()) {
            // Run
            this.runEmptyTasks();
            if (whenDone != null) {
                whenDone.run();
            }
            return true;
        }
        if (whenDone != null) {
            this.emptyTasks.add(whenDone);
        }
        return false;
    }

    private synchronized boolean runEmptyTasks() {
        if (this.emptyTasks.isEmpty()) {
            FaweCache.cleanAll(); // clean when empty ???
            return false;
        }
        final ConcurrentLinkedDeque<Runnable> tmp = new ConcurrentLinkedDeque<>(this.emptyTasks);
        this.emptyTasks.clear();
        for (final Runnable runnable : tmp) {
            runnable.run();
        }
        return true;
    }
}
