package com.fastasyncworldedit.bukkit.adapter;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.fastasyncworldedit.core.math.IntPair;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.fastasyncworldedit.core.queue.implementation.QueueHandler;
import com.fastasyncworldedit.core.queue.implementation.blocks.CharGetBlocks;
import com.fastasyncworldedit.core.util.MemUtil;
import com.fastasyncworldedit.core.util.task.FaweThreadUtil;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractBukkitGetBlocks<ServerLevel, LevelChunk> extends CharGetBlocks {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    protected final ServerLevel serverLevel;
    protected final int chunkX;
    protected final int chunkZ;
    protected final ReentrantLock callLock = new ReentrantLock();
    protected final ConcurrentHashMap<Integer, IChunkGet> copies = new ConcurrentHashMap<>();
    // Completes when the post-processors of the last write to this chunk have run. The next write runs its
    // post-processors after it, so post-processors see the writes to one chunk in the order they were made.
    private final AtomicReference<CompletableFuture<Void>> lastPostProcess = new AtomicReference<>(
            CompletableFuture.completedFuture(null));
    protected final IntPair chunkPos;
    protected final int minHeight;
    protected final int maxHeight;
    protected boolean createCopy = false;
    protected boolean forceLoadSections = true;
    protected int copyKey = 0;

    protected AbstractBukkitGetBlocks(
            ServerLevel serverLevel, int chunkX, int chunkZ, int minY, int maxY
    ) {
        super(minY >> 4, maxY >> 4);
        this.serverLevel = serverLevel;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.minHeight = minY;
        this.maxHeight = maxY; // Minecraft max limit is exclusive
        this.chunkPos = new IntPair(chunkX, chunkZ);
    }

    protected abstract void send();

    protected abstract CompletableFuture<LevelChunk> ensureLoaded(ServerLevel serverLevel);

    protected abstract <T extends Future<T>> T internalCall(
            IChunkSet set,
            Runnable finalizer,
            int copyKey,
            LevelChunk nmsChunk,
            ServerLevel nmsWorld
    ) throws Exception;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public synchronized <T extends Future<T>> T call(IQueueExtent<? extends IChunk> owner, IChunkSet set, Runnable finalizer) {
        if (!callLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Attempted to call chunk GET but chunk was not call-locked.");
        }
        forceLoadSections = false;
        final ServerLevel nmsWorld = serverLevel;
        CompletableFuture<LevelChunk> nmsChunkFuture = ensureLoaded(nmsWorld);
        LevelChunk chunk = nmsChunkFuture.getNow(null);
        if ((chunk == null && MemUtil.shouldBeginSlow()) || Settings.settings().QUEUE.ASYNC_CHUNK_LOAD_WRITE) {
            try {
                // "Artificially" slow FAWE down if memory low as performing the operation async can cause large amounts of
                // memory usage
                chunk = nmsChunkFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("Could not get chunk at {},{} whilst low memory", chunkX, chunkZ, e);
                throw new FaweException(
                        TextComponent.of("Could not get chunk at " + chunkX + "," + chunkZ + " whilst low memory: " + e.getMessage()));
            }
        }
        final int finalCopyKey = copyKey;
        // A copy is created exactly when the edit has post-processors
        final boolean postProcess = createCopy;
        // Run immediately if possible
        if (chunk != null) {
            return tryInternalCall(set, finalizer, finalCopyKey, chunk, nmsWorld, postProcess);
        }
        // Submit via the STQE as that will help handle excessive queuing by waiting for the submission count to fall below the
        // target size
        final Extent extent = FaweThreadUtil.getCurrentExtent();
        nmsChunkFuture.thenApply(nmsChunk -> owner.submitTaskUnchecked(() -> (T) tryWrappedInternalCall(
                set,
                finalizer,
                finalCopyKey,
                nmsChunk,
                nmsWorld,
                postProcess,
                extent
        )));
        // If we have re-submitted, return a completed future to prevent potential deadlocks where a future reliant on the
        // above submission is halting the BlockingExecutor, and preventing the above task from actually running. The futures
        // submitted above will still be added to the STQE submissions.
        return (T) (Future) CompletableFuture.completedFuture(null);
    }

    private <T extends Future<T>> T tryWrappedInternalCall(
            IChunkSet set,
            Runnable finalizer,
            int copyKey,
            LevelChunk nmsChunk,
            ServerLevel nmsWorld,
            boolean postProcess,
            Extent extent
    ) {
        FaweThreadUtil.setCurrentExtent(extent);
        try {
            return tryInternalCall(set, finalizer, copyKey, nmsChunk, nmsWorld, postProcess);
        } finally {
            FaweThreadUtil.clearCurrentExtent();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T extends Future<T>> T tryInternalCall(
            IChunkSet set,
            Runnable finalizer,
            int copyKey,
            LevelChunk nmsChunk,
            ServerLevel nmsWorld,
            boolean postProcess
    ) {
        // A write on the main thread is not ordered: its queue may wait on the main thread for the post-processors, and the
        // post-processors of an earlier write may wait for the main thread
        OrderedFinalizer ordered = null;
        if (postProcess && !Fawe.isMainThread()) {
            CompletableFuture<Void> done = new CompletableFuture<>();
            ordered = new OrderedFinalizer(lastPostProcess.getAndSet(done), done, finalizer);
        }
        try {
            if (ordered == null) {
                return internalCall(set, finalizer, copyKey, nmsChunk, nmsWorld);
            }
            final T result = internalCall(set, ordered, copyKey, nmsChunk, nmsWorld);
            // The queue waits until the post-processors of this write have run, as it does when they run unordered
            return (T) (Future) ordered.done.thenApply(ignored -> result);
        } catch (Throwable e) {
            if (ordered != null) {
                ordered.skip();
            }
            LOGGER.error("Error performing chunk call at chunk {},{}", chunkX, chunkZ, e);
            return null;
        } finally {
            forceLoadSections = true;
        }
    }

    protected <T extends Future<T>> T handleCallFinalizer(
            final List<Runnable> syncTasks,
            final Runnable callback,
            final Runnable finalizer
    ) throws
            Exception {
        if (!syncTasks.isEmpty()) {
            QueueHandler queueHandler = Fawe.instance().getQueueHandler();

            // Chain the sync tasks and the callback
            Callable<Future<?>> chain = () -> {
                try {
                    // Run the sync tasks
                    for (Runnable task : syncTasks) {
                        if (task != null) {
                            task.run();
                        }
                    }
                    if (callback != null) {
                        return queueHandler.async(() -> {
                            try {
                                callback.run();
                            } finally {
                                // The callback may throw before it runs the finalizer
                                skip(finalizer);
                            }
                        }, null);
                    } else if (finalizer != null) {
                        return queueHandler.async(finalizer, null);
                    }
                    return null;
                } catch (Throwable e) {
                    skip(finalizer);
                    LOGGER.error("Error performing final chunk calling at {},{}", chunkX, chunkZ, e);
                    throw e;
                }
            };
            //noinspection unchecked - required at compile time
            return (T) (Future) queueHandler.sync(chain);
        } else {
            if (callback != null) {
                callback.run();
            } else if (finalizer != null) {
                finalizer.run();
            }
        }
        return null;
    }

    @Override
    public int getX() {
        return chunkX;
    }

    @Override
    public int getZ() {
        return chunkZ;
    }

    @Override
    public boolean isCreateCopy() {
        return createCopy;
    }

    @Override
    public int setCreateCopy(boolean createCopy) {
        if (!callLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Attempting to set if chunk GET should create copy, but it is not call-locked.");
        }
        this.createCopy = createCopy;
        // Increment regardless of whether copy will be created or not to return null from getCopy()
        return ++this.copyKey;
    }

    @Override
    public IChunkGet getCopy(final int key) {
        return copies.remove(key);
    }

    @Override
    public void lockCall() {
        this.callLock.lock();
    }

    @Override
    public void unlockCall() {
        this.callLock.unlock();
    }

    @Override
    public int getMaxY() {
        return maxHeight;
    }

    @Override
    public int getMinY() {
        return minHeight;
    }

    private static void skip(Runnable finalizer) {
        if (finalizer instanceof OrderedFinalizer ordered) {
            ordered.skip();
        }
    }

    /**
     * Runs a finalizer, which runs the post-processors, after the finalizer of the previous write to the same chunk. It chains
     * on that write and never blocks a thread. A path that will never run the finalizer must call {@link #skip()}, so that the
     * next write does not wait for it.
     */
    private static final class OrderedFinalizer implements Runnable {

        private final CompletableFuture<Void> previous;
        private final CompletableFuture<Void> done;
        private final Runnable finalizer;
        private final AtomicBoolean claimed = new AtomicBoolean();

        private OrderedFinalizer(CompletableFuture<Void> previous, CompletableFuture<Void> done, Runnable finalizer) {
            this.previous = previous;
            this.done = done;
            this.finalizer = finalizer;
        }

        @Override
        public void run() {
            if (claimed.compareAndSet(false, true)) {
                previous.whenComplete((ignored, error) -> {
                    // The previous write completes on the main thread if its sync tasks fail. Post-processors do not run there.
                    if (Fawe.isMainThread()) {
                        Fawe.instance().getQueueHandler().async(this::finish);
                    } else {
                        finish();
                    }
                });
            }
        }

        private void finish() {
            try {
                finalizer.run();
                done.complete(null);
            } catch (Throwable e) {
                done.completeExceptionally(e);
            }
        }

        private void skip() {
            if (claimed.compareAndSet(false, true)) {
                done.complete(null);
            }
        }

    }

}
