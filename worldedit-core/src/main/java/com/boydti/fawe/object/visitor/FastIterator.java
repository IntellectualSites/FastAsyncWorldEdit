package com.boydti.fawe.object.visitor;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.MappedFaweQueue;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.util.ExtentTraverser;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import java.util.Iterator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FastIterator implements Iterable<Vector> {

    private final Iterable<? extends Vector> iterable;
    private final MappedFaweQueue queue;

    public FastIterator(@Nonnull Iterable<? extends Vector> iter, @Nullable EditSession extent) {
        this(iter, (HasFaweQueue) extent);
    }

    public FastIterator(@Nonnull Iterable<? extends Vector> iter, @Nullable Extent extent) {
        this(iter, (HasFaweQueue) (extent != null ? (extent instanceof HasFaweQueue ? extent : new ExtentTraverser(extent).findAndGet(HasFaweQueue.class)) : null));
    }

    public FastIterator(@Nonnull Iterable<? extends Vector> iter, @Nullable HasFaweQueue editSession) {
        this(iter, (FaweQueue) (editSession != null ? editSession.getQueue() : null));
    }

    public FastIterator(@Nonnull Iterable<? extends Vector> iter, @Nullable FaweQueue faweQueue) {
        this.iterable = iter;
        this.queue = faweQueue != null && faweQueue instanceof MappedFaweQueue ? (MappedFaweQueue) faweQueue : null;
    }

    public Iterable<? extends Vector> getIterable() {
        return iterable;
    }

    @Override
    public Iterator<Vector> iterator() {
        if (queue == null || Settings.IMP.QUEUE.PRELOAD_CHUNKS <= 1) {
            return (Iterator<Vector>) iterable.iterator();
        }
        return new Iterator<Vector>() {
            Iterator<? extends Vector> trailIter = iterable.iterator();
            Iterator<? extends Vector> leadIter = iterable.iterator();
            int lastTrailChunkX = Integer.MIN_VALUE;
            int lastTrailChunkZ = Integer.MIN_VALUE;
            int lastLeadChunkX = Integer.MIN_VALUE;
            int lastLeadChunkZ = Integer.MIN_VALUE;
            int loadingTarget = Settings.IMP.QUEUE.PRELOAD_CHUNKS;
            int cx, cz;

            @Override
            public void remove() {
                trailIter.remove();
            }

            @Override
            public boolean hasNext() {
                return trailIter.hasNext();
            }

            @Override
            public Vector next() {
                Vector pt = trailIter.next();
                if (lastTrailChunkX != (lastTrailChunkX = pt.getBlockX() >> 4) || lastTrailChunkZ != (lastTrailChunkZ = pt.getBlockZ() >> 4)) {
                    if (leadIter.hasNext()) {
                        try {
                            int amount;
                            if (lastLeadChunkX == Integer.MIN_VALUE) {
                                lastLeadChunkX = cx;
                                lastLeadChunkZ = cz;
                                amount = loadingTarget;
                            } else {
                                amount = 1;
                            }
                            for (int count = 0; count < amount; ) {
                                Vector v = leadIter.next();
                                int vcx = v.getBlockX() >> 4;
                                int vcz = v.getBlockZ() >> 4;
                                if (vcx != lastLeadChunkX || vcz != lastLeadChunkZ) {
                                    lastLeadChunkX = vcx;
                                    lastLeadChunkZ = vcz;
                                    queue.queueChunkLoad(vcx, vcz);
                                    count++;
                                }
                            }
                        } catch (Throwable ignore) {
                        }
                    }
                }
                return pt;
            }
        };
    }
}
