package com.boydti.fawe.object.visitor;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.MappedFaweQueue;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.util.ExtentTraverser;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;

import java.util.Iterator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FastChunkIterator implements Iterable<BlockVector2> {

    private final Iterable<? extends BlockVector2> iterable;
    private final MappedFaweQueue queue;

    public FastChunkIterator(@Nonnull Iterable<? extends BlockVector2> iter, @Nullable EditSession extent) {
        this(iter, (HasFaweQueue) extent);
    }

    public FastChunkIterator(@Nonnull Iterable<? extends BlockVector2> iter, @Nullable Extent extent) {
        this(iter, (HasFaweQueue) (extent != null ? (extent instanceof HasFaweQueue ? extent : new ExtentTraverser(extent).findAndGet(HasFaweQueue.class)) : null));
    }

    public FastChunkIterator(@Nonnull Iterable<? extends BlockVector2> iter, @Nullable HasFaweQueue editSession) {
        this(iter, (FaweQueue) (editSession != null ? editSession.getQueue() : null));
    }

    public FastChunkIterator(@Nonnull Iterable<? extends BlockVector2> iter, @Nullable FaweQueue faweQueue) {
        this.iterable = iter;
        this.queue = faweQueue != null && faweQueue instanceof MappedFaweQueue ? (MappedFaweQueue) faweQueue : null;
    }

    public Iterable<? extends BlockVector2> getIterable() {
        return iterable;
    }

    @Override
    public Iterator<BlockVector2> iterator() {
        if (queue == null || Settings.IMP.QUEUE.PRELOAD_CHUNKS <= 1) {
            return (Iterator<BlockVector2>) iterable.iterator();
        }
        final Iterator<? extends BlockVector2> trailIter = iterable.iterator();
        final Iterator<? extends BlockVector2> leadIter = iterable.iterator();
        int amount = Settings.IMP.QUEUE.PRELOAD_CHUNKS;
        for (int i = 0; i < Settings.IMP.QUEUE.PRELOAD_CHUNKS && leadIter.hasNext(); i++) {
            BlockVector2 toLoad = leadIter.next();
            queue.queueChunkLoad(toLoad.getBlockX(), toLoad.getBlockZ());
        }
        if (!leadIter.hasNext()) {
            return (Iterator<BlockVector2>) trailIter;
        }
        return new Iterator<BlockVector2>() {
            @Override
            public void remove() {
                trailIter.remove();
            }

            @Override
            public boolean hasNext() {
                return trailIter.hasNext();
            }

            @Override
            public BlockVector2 next() {
                if (leadIter.hasNext()) {
                    BlockVector2 toLoad = leadIter.next();
                    queue.queueChunkLoad(toLoad.getBlockX(), toLoad.getBlockZ());
                }
                return trailIter.next();
            }
        };
    }
}
