package com.fastasyncworldedit.core.queue.implementation;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunkCache;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.IQueueChunk;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.fastasyncworldedit.core.queue.Trimable;
import com.fastasyncworldedit.core.queue.implementation.chunk.ChunkCache;
import com.fastasyncworldedit.core.util.MemUtil;
import com.fastasyncworldedit.core.util.TaskManager;
import com.fastasyncworldedit.core.util.collection.CleanableThreadLocal;
import com.fastasyncworldedit.core.util.task.FaweForkJoinWorkerThreadFactory;
import com.fastasyncworldedit.core.wrappers.WorldWrapper;
import com.google.common.util.concurrent.Futures;
import com.sk89q.worldedit.world.World;
import org.jetbrains.annotations.ApiStatus;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

/**
 * Class which handles all the queues {@link IQueueExtent}
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class QueueHandler implements Trimable, Runnable {

    /**
     * Primary queue should be used for tasks that are unlikely to wait on other tasks, IO, etc. (i.e. spend most of their
     * time utilising CPU.
     */
    private final ForkJoinPool forkJoinPoolPrimary = new ForkJoinPool(
            Settings.settings().QUEUE.PARALLEL_THREADS,
            new FaweForkJoinWorkerThreadFactory("FAWE Fork Join Pool Primary - %s"),
            null,
            false
    );

    /**
     * Secondary queue should be used for "cleanup" tasks that are likely to be shorter in life than those submitted to the
     * primary queue. They may be IO-bound tasks.
     */
    private final ForkJoinPool forkJoinPoolSecondary = new ForkJoinPool(
            Settings.settings().QUEUE.PARALLEL_THREADS,
            new FaweForkJoinWorkerThreadFactory("FAWE Fork Join Pool Secondary - %s"),
            null,
            false
    );
    /**
     * Main "work-horse" queue for FAWE. Handles chunk submission (and chunk submission alone). Blocking in order to forcibly
     * prevent overworking/over-submission of chunk process tasks.
     */
    private final ThreadPoolExecutor blockingExecutor = FaweCache.INSTANCE.newBlockingExecutor(
            "FAWE QueueHandler Blocking Executor - %d");
    /**
     * Queue for tasks to be completed on the main thread. These take priority of tasks submitted to syncWhenFree queue
     */
    private final ConcurrentLinkedQueue<FutureTask> syncTasks = new ConcurrentLinkedQueue<>();
    /**
     * Queue for tasks to be completed on the main thread. These are completed only if and when there is time left in a tick
     * after completing all tasks in the syncTasks queue
     */
    private final ConcurrentLinkedQueue<FutureTask> syncWhenFree = new ConcurrentLinkedQueue<>();

    private final Map<World, WeakReference<IChunkCache<IChunkGet>>> chunkGetCache = new HashMap<>();
    private final CleanableThreadLocal<IQueueExtent<IQueueChunk>> queuePool = new CleanableThreadLocal<>(QueueHandler.this::create);
    /**
     * Used to calculate elapsed time in milliseconds and ensure block placement doesn't lag the
     * server
     */
    private long last;
    private long allocate = 50;

    protected QueueHandler() {
        TaskManager.taskManager().repeat(this, 1);
    }

    @ApiStatus.Internal
    public ThreadPoolExecutor getBlockingExecutor() {
        return blockingExecutor;
    }

    @Override
    public void run() {
        if (!Fawe.isMainThread()) {
            throw new IllegalStateException("Not main thread");
        }
        if (!syncTasks.isEmpty()) {
            long currentAllocate = getAllocate();

            if (!MemUtil.isMemoryFree()) {
                // TODO reduce mem usage
                // FaweCache trim
                // Preloader trim
            }

            operate(syncTasks, last, currentAllocate);
        } else if (!syncWhenFree.isEmpty()) {
            operate(syncWhenFree, last, getAllocate());
        } else {
            // trim??
        }
    }

    /**
     * Get if the {@code blockingExecutor} is saturated with tasks or not. Under-utilisation implies the queue has space for
     * more submissions.
     *
     * @return true if {@code blockingExecutor} is not saturated with tasks
     */
    public boolean isUnderutilized() {
        return blockingExecutor.getActiveCount() < blockingExecutor.getMaximumPoolSize();
    }

    private long getAllocate() {
        long now = System.currentTimeMillis();
        double targetTPS = 18 - Math.max(Settings.settings().QUEUE.EXTRA_TIME_MS * 0.05, 0);
        long diff = 50 + this.last - (this.last = now);
        long absDiff = Math.abs(diff);
        if (diff == 0) {
            allocate = Math.min(50, allocate + 1);
        } else if (diff < 0) {
            allocate = Math.max(5, allocate + diff);
        } else if (!Fawe.instance().getTimer().isAbove(targetTPS)) {
            allocate = Math.max(5, allocate - 1);
        }
        return allocate - absDiff;
    }

    private void operate(Queue<FutureTask> queue, long start, long currentAllocate) {
        boolean wait = false;
        do {
            Runnable task = queue.poll();
            if (task == null) {
                if (wait) {
                    synchronized (syncTasks) {
                        try {
                            queue.wait(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    task = queue.poll();
                    wait = false;
                } else {
                    break;
                }
            }
            if (task != null) {
                task.run();
                wait = true;
            }
        } while (System.currentTimeMillis() - start < currentAllocate);
    }

    /**
     * @deprecated For removal without replacement.
     */
    @Deprecated(forRemoval = true, since = "2.6.2")
    public <T extends Future<T>> void complete(Future<T> task) {
        try {
            while (task != null) {
                task = task.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Complete a task in the {@code forkJoinPoolSecondary} queue. Secondary queue should be used for "cleanup" tasks that are
     * likely to be shorter in life than those submitted to the primary queue. They may be IO-bound tasks.
     *
     * @param run   Runnable to run
     * @param value Value to return when done
     * @param <T>   Value type
     * @return Future for submitted task
     */
    public <T> Future<T> async(Runnable run, T value) {
        return forkJoinPoolSecondary.submit(run, value);
    }

    /**
     * Complete a task in the {@code forkJoinPoolSecondary} queue. Secondary queue should be used for "cleanup" tasks that are
     * likely to be shorter in life than those submitted to the primary queue. They may be IO-bound tasks.
     *
     * @param run Runnable to run
     * @return Future for submitted task
     */
    public Future<?> async(Runnable run) {
        return forkJoinPoolSecondary.submit(run);
    }

    /**
     * Complete a task in the {@code forkJoinPoolSecondary} queue. Secondary queue should be used for "cleanup" tasks that are
     * likely to be shorter in life than those submitted to the primary queue. They may be IO-bound tasks.
     *
     * @param call Callable to run
     * @param <T>  Return value type
     * @return Future for submitted task
     */
    public <T> Future<T> async(Callable<T> call) {
        return forkJoinPoolSecondary.submit(call);
    }

    /**
     * Complete a task in the {@code forkJoinPoolPrimary} queue. Primary queue should be used for tasks that are unlikely to
     * wait on other tasks, IO, etc. (i.e. spend most of their time utilising CPU.
     *
     * @param run Task to run
     * @return {@link ForkJoinTask} representing task being run
     */
    public ForkJoinTask submit(Runnable run) {
        return forkJoinPoolPrimary.submit(run);
    }

    /**
     * Submit a task to be run on the main thread. Does not guarantee to be run on the next tick as FAWE will only operate to
     * maintain approx. 18 tps.
     *
     * @param run Task to run
     * @param <T> Value type
     * @return Future representing task
     */
    public <T> Future<T> sync(Runnable run) {
        return sync(run, syncTasks);
    }

    /**
     * Submit a task to be run on the main thread. Does not guarantee to be run on the next tick as FAWE will only operate to
     * maintain approx. 18 tps.
     *
     * @param call Task to run
     * @param <T>  Value type
     * @return Future representing task
     */
    public <T> Future<T> sync(Callable<T> call) throws Exception {
        return sync(call, syncTasks);
    }

    /**
     * Submit a task to be run on the main thread. Does not guarantee to be run on the next tick as FAWE will only operate to
     * maintain approx. 18 tps.
     *
     * @param supplier Task to run
     * @param <T>      Value type
     * @return Future representing task
     */
    public <T> Future<T> sync(Supplier<T> supplier) {
        return sync(supplier, syncTasks);
    }

    /**
     * Submit a task to be run on the main thread. Does not guarantee to be run on the next tick as FAWE will only operate to
     * maintain approx. 18 tps. Takes lower priority than tasks submitted via any {@code QueueHandler#sync} method. Completed
     * only if and when there is time left in a tick after completing all sync tasks submitted using the aforementioned methods.
     *
     * @param run   Task to run
     * @param value Value to return when done
     * @param <T>   Value type
     * @return Future representing task
     */
    public <T> Future<T> syncWhenFree(Runnable run, T value) {
        return sync(run, value, syncWhenFree);
    }

    /**
     * Submit a task to be run on the main thread. Does not guarantee to be run on the next tick as FAWE will only operate to
     * maintain approx. 18 tps. Takes lower priority than tasks submitted via any {@code QueueHandler#sync} method. Completed
     * only if and when there is time left in a tick after completing all sync tasks submitted using the aforementioned methods.
     *
     * @param run Task to run
     * @param <T> Value type
     * @return Future representing task
     */
    public <T> Future<T> syncWhenFree(Runnable run) {
        return sync(run, syncWhenFree);
    }

    /**
     * Submit a task to be run on the main thread. Does not guarantee to be run on the next tick as FAWE will only operate to
     * maintain approx. 18 tps. Takes lower priority than tasks submitted via any {@code QueueHandler#sync} method. Completed
     * only if and when there is time left in a tick after completing all sync tasks submitted using the aforementioned methods.
     *
     * @param call Task to run
     * @param <T>  Value type
     * @return Future representing task
     */
    public <T> Future<T> syncWhenFree(Callable<T> call) throws Exception {
        return sync(call, syncWhenFree);
    }

    /**
     * Submit a task to be run on the main thread. Does not guarantee to be run on the next tick as FAWE will only operate to
     * maintain approx. 18 tps. Takes lower priority than tasks submitted via any {@code QueueHandler#sync} method. Completed
     * only if and when there is time left in a tick after completing all sync tasks submitted using the aforementioned methods.
     *
     * @param supplier Task to run
     * @param <T>      Value type
     * @return Future representing task
     */
    public <T> Future<T> syncWhenFree(Supplier<T> supplier) {
        return sync(supplier, syncWhenFree);
    }

    private <T> Future<T> sync(Runnable run, T value, Queue<FutureTask> queue) {
        if (Fawe.isMainThread()) {
            run.run();
            return Futures.immediateFuture(value);
        }
        final FutureTask<T> result = new FutureTask<>(run, value);
        queue.add(result);
        notifySync(queue);
        return result;
    }

    private <T> Future<T> sync(Runnable run, Queue<FutureTask> queue) {
        if (Fawe.isMainThread()) {
            run.run();
            return Futures.immediateCancelledFuture();
        }
        final FutureTask<T> result = new FutureTask<>(run, null);
        queue.add(result);
        notifySync(queue);
        return result;
    }

    private <T> Future<T> sync(Callable<T> call, Queue<FutureTask> queue) throws Exception {
        if (Fawe.isMainThread()) {
            return Futures.immediateFuture(call.call());
        }
        final FutureTask<T> result = new FutureTask<>(call);
        queue.add(result);
        notifySync(queue);
        return result;
    }

    private <T> Future<T> sync(Supplier<T> call, Queue<FutureTask> queue) {
        if (Fawe.isMainThread()) {
            return Futures.immediateFuture(call.get());
        }
        final FutureTask<T> result = new FutureTask<>(call::get);
        queue.add(result);
        notifySync(queue);
        return result;
    }

    private void notifySync(Object object) {
        synchronized (object) {
            object.notifyAll();
        }
    }

    /**
     * Internal use only. Specifically for submitting {@link IQueueChunk} for "processing" an edit. Submits to the blocking
     * executor, the main "work-horse" queue for FAWE. Handles chunk submission (and chunk submission alone). Blocking in order
     * to forcibly prevent overworking/over-submission of chunk process tasks.
     *
     * @param chunk chunk
     * @param <T>
     * @return Future representing task
     */
    public <T extends Future<T>> T submit(IQueueChunk<T> chunk) {
//        if (MemUtil.isMemoryFree()) { TODO NOT IMPLEMENTED - optimize this
//            return (T) forkJoinPoolSecondary.submit(chunk);
//        }
        return (T) blockingExecutor.submit(chunk);
    }

    @ApiStatus.Internal
    public <T extends Future<T>> T submitToBlocking(Callable<T> callable) {
        return (T) blockingExecutor.submit(callable);
    }

    /**
     * Get or create the WorldChunkCache for a world
     */
    public IChunkCache<IChunkGet> getOrCreateWorldCache(World world) {
        world = WorldWrapper.unwrap(world);

        synchronized (chunkGetCache) {
            final WeakReference<IChunkCache<IChunkGet>> ref = chunkGetCache.get(world);
            if (ref != null) {
                final IChunkCache<IChunkGet> cached = ref.get();
                if (cached != null) {
                    return cached;
                }
            }
            final IChunkCache<IChunkGet> created = new ChunkCache<>(world);
            chunkGetCache.put(world, new WeakReference<>(created));
            return created;
        }
    }

    public IQueueExtent<IQueueChunk> create() {
        return new SingleThreadQueueExtent();
    }

    /**
     * Sets the current thread's {@link IQueueExtent} instance in the queue pool to null.
     */
    public void unCache() {
        queuePool.remove();
    }

    private IQueueExtent<IQueueChunk> pool() {
        IQueueExtent<IQueueChunk> queue = queuePool.get();
        if (queue == null) {
            queuePool.set(queue = queuePool.init());
        }
        return queue;
    }

    /**
     * Indicate a "set" task is being started.
     *
     * @param parallel if the "set" being started is parallel/async
     * @deprecated To be replaced by better-named {@link QueueHandler#startUnsafe(boolean)} )}
     */
    @Deprecated(forRemoval = true, since = "2.6.2")
    public void startSet(boolean parallel) {
        startUnsafe(parallel);
    }


    /**
     * Indicate a "set" task is ending.
     *
     * @param parallel if the "set" being started is parallel/async
     * @deprecated To be replaced by better-named {@link QueueHandler#endUnsafe(boolean)} )}
     */
    @Deprecated(forRemoval = true, since = "2.6.2")
    public void endSet(boolean parallel) {
        startUnsafe(parallel);
    }

    /**
     * Indicate an unsafe task is starting. Physics are frozen, async catchers disabled, etc. for the duration of the task
     *
     * @param parallel If the task is being run async and/or in parallel
     */
    public abstract void startUnsafe(boolean parallel);

    /**
     * Indicate a/the unsafe task submitted after a {@link QueueHandler#startUnsafe(boolean)} call has ended.
     *
     * @param parallel If the task was being run async and/or in parallel
     */
    public abstract void endUnsafe(boolean parallel);

    /**
     * Create a new queue for a given world.
     */
    public IQueueExtent<IQueueChunk> getQueue(World world) {
        return getQueue(world, null, null);
    }

    /**
     * Create a new queue for a given world.
     *
     * @param world         World to create queue for
     * @param processor     existing processor to set to queue or null
     * @param postProcessor existing post-processor to set to queue or null
     * @return New queue for given world
     */
    public IQueueExtent<IQueueChunk> getQueue(World world, IBatchProcessor processor, IBatchProcessor postProcessor) {
        final IQueueExtent<IQueueChunk> queue = pool();
        IChunkCache<IChunkGet> cacheGet = getOrCreateWorldCache(world);
        IChunkCache<IChunkSet> set = null; // TODO cache?
        queue.init(world, cacheGet, set);
        if (processor != null) {
            queue.setProcessor(processor);
        }
        if (postProcessor != null) {
            queue.setPostProcessor(postProcessor);
        }
        return queue;
    }

    /**
     * Trims each chunk GET cache
     *
     * @param aggressive if each chunk GET cache should be trimmed aggressively
     * @return true if all chunk GET caches could be trimmed
     */
    @Override
    public boolean trim(boolean aggressive) {
        boolean result = true;
        synchronized (chunkGetCache) {
            final Iterator<Map.Entry<World, WeakReference<IChunkCache<IChunkGet>>>> iter = chunkGetCache
                    .entrySet().iterator();
            while (iter.hasNext()) {
                final Map.Entry<World, WeakReference<IChunkCache<IChunkGet>>> entry = iter.next();
                final WeakReference<IChunkCache<IChunkGet>> value = entry.getValue();
                final IChunkCache<IChunkGet> cache = value.get();
                if (cache.trim(aggressive)) {
                    iter.remove();
                    continue;
                }
                result = false;
            }
        }
        return result;
    }

    /**
     * Primary queue should be used for tasks that are unlikely to wait on other tasks, IO, etc. (i.e. spend most of their
     * time utilising CPU.
     * <p>
     * Internal API usage only.
     *
     * @since 2.7.0
     */
    public ExecutorService getForkJoinPoolPrimary() {
        return forkJoinPoolPrimary;
    }

    /**
     * Secondary queue should be used for "cleanup" tasks that are likely to be shorter in life than those submitted to the
     * primary queue. They may be IO-bound tasks.
     * <p>
     * Internal API usage only.
     *
     * @since 2.7.0
     */
    public ExecutorService getForkJoinPoolSecondary() {
        return forkJoinPoolSecondary;
    }

}
