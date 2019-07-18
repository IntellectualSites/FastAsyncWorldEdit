package com.boydti.fawe.object.visitor;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.MappedIQueueExtent;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.object.HasIQueueExtent;
import com.boydti.fawe.util.ExtentTraverser;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FastChunkIterator implements Iterable<BlockVector2> {

    private final Iterable<? extends BlockVector2> iterable;
    private final MappedIQueueExtent queue;

    public FastChunkIterator(@Nonnull Iterable<? extends BlockVector2> iter, @Nullable EditSession extent) {
        this(iter, (HasIQueueExtent) extent);
    }

    public FastChunkIterator(@Nonnull Iterable<? extends BlockVector2> iter, @Nullable Extent extent) {
        this(iter, (HasIQueueExtent) (extent != null ? (extent instanceof HasIQueueExtent ? extent : new ExtentTraverser(extent).findAndGet(HasIQueueExtent.class)) : null));
    }

    public FastChunkIterator(@Nonnull Iterable<? extends BlockVector2> iter, @Nullable HasIQueueExtent editSession) {
        this(iter, editSession != null ? editSession.getQueue() : null);
    }

    public FastChunkIterator(@Nonnull Iterable<? extends BlockVector2> iter, @Nullable IQueueExtent IQueueExtent) {
        this.iterable = iter;
        this.queue = IQueueExtent instanceof MappedIQueueExtent ? (MappedIQueueExtent) IQueueExtent : null;
    }

    public Iterable<? extends BlockVector2> getIterable() {
        return iterable;
    }

    @NotNull
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
