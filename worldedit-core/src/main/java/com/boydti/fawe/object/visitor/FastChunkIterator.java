package com.boydti.fawe.object.visitor;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.MappedFaweQueue;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.util.ExtentTraverser;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.extent.Extent;
import java.util.Iterator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FastChunkIterator implements Iterable<Vector2D> {

    private final Iterable<? extends Vector2D> iterable;
    private final MappedFaweQueue queue;

    public FastChunkIterator(@Nonnull Iterable<? extends Vector2D> iter, @Nullable EditSession extent) {
        this(iter, (HasFaweQueue) extent);
    }

    public FastChunkIterator(@Nonnull Iterable<? extends Vector2D> iter, @Nullable Extent extent) {
        this(iter, (HasFaweQueue) (extent != null ? (extent instanceof HasFaweQueue ? extent : new ExtentTraverser(extent).findAndGet(HasFaweQueue.class)) : null));
    }

    public FastChunkIterator(@Nonnull Iterable<? extends Vector2D> iter, @Nullable HasFaweQueue editSession) {
        this(iter, (FaweQueue) (editSession != null ? editSession.getQueue() : null));
    }

    public FastChunkIterator(@Nonnull Iterable<? extends Vector2D> iter, @Nullable FaweQueue faweQueue) {
        this.iterable = iter;
        this.queue = faweQueue != null && faweQueue instanceof MappedFaweQueue ? (MappedFaweQueue) faweQueue : null;
    }

    public Iterable<? extends Vector2D> getIterable() {
        return iterable;
    }

    @Override
    public Iterator<Vector2D> iterator() {
        if (queue == null || Settings.IMP.QUEUE.PRELOAD_CHUNKS <= 1) {
            return (Iterator<Vector2D>) iterable.iterator();
        }
        final Iterator<? extends Vector2D> trailIter = iterable.iterator();
        final Iterator<? extends Vector2D> leadIter = iterable.iterator();
        int amount = Settings.IMP.QUEUE.PRELOAD_CHUNKS;
        for (int i = 0; i < Settings.IMP.QUEUE.PRELOAD_CHUNKS && leadIter.hasNext(); i++) {
            Vector2D toLoad = leadIter.next();
            queue.queueChunkLoad(toLoad.getBlockX(), toLoad.getBlockZ());
        }
        if (!leadIter.hasNext()) {
            return (Iterator<Vector2D>) trailIter;
        }
        return new Iterator<Vector2D>() {
            @Override
            public void remove() {
                trailIter.remove();
            }

            @Override
            public boolean hasNext() {
                return trailIter.hasNext();
            }

            @Override
            public Vector2D next() {
                if (leadIter.hasNext()) {
                    Vector2D toLoad = leadIter.next();
                    queue.queueChunkLoad(toLoad.getBlockX(), toLoad.getBlockZ());
                }
                return trailIter.next();
            }
        };
    }
}
