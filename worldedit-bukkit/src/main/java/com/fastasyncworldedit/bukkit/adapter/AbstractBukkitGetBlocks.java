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
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractBukkitGetBlocks<ServerLevel, LevelChunk> extends CharGetBlocks {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    protected final ServerLevel serverLevel;
    protected final int chunkX;
    protected final int chunkZ;
    protected final ReentrantLock callLock = new ReentrantLock();
    protected final ConcurrentHashMap<Integer, IChunkGet> copies = new ConcurrentHashMap<>();
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
        // Run immediately if possible
        if (chunk != null) {
            return tryWrappedInternalCall(set, finalizer, finalCopyKey, chunk, nmsWorld);
        }
        // Submit via the STQE as that will help handle excessive queuing by waiting for the submission count to fall below the
        // target size
        nmsChunkFuture.thenApply(nmsChunk -> owner.submitTaskUnchecked(() -> (T) tryWrappedInternalCall(
                set,
                finalizer,
                finalCopyKey,
                nmsChunk,
                nmsWorld
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
            ServerLevel nmsWorld
    ) {
        try {
            return internalCall(set, finalizer, copyKey, nmsChunk, nmsWorld);
        } catch (Throwable e) {
            LOGGER.error("Error performing chunk call at chunk {},{}", chunkX, chunkZ, e);
            return null;
        } finally {
            forceLoadSections = true;
        }
    }

    protected <T extends Future<T>> T handleCallFinalizer(Runnable[] syncTasks, Runnable callback, Runnable finalizer) throws
            Exception {
        if (syncTasks != null) {
            QueueHandler queueHandler = Fawe.instance().getQueueHandler();
            Runnable[] finalSyncTasks = syncTasks;

            // Chain the sync tasks and the callback
            Callable<Future<?>> chain = () -> {
                try {
                    // Run the sync tasks
                    for (Runnable task : finalSyncTasks) {
                        if (task != null) {
                            task.run();
                        }
                    }
                    if (callback != null) {
                        return queueHandler.async(callback, null);
                    } else if (finalizer != null) {
                        return queueHandler.async(finalizer, null);
                    }
                    return null;
                } catch (Throwable e) {
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

}
