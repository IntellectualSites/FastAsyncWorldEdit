package com.boydti.fawe.object.visitor;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.MappedFaweQueue;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.util.ExtentTraverser;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.Iterator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FastIterator implements Iterable<BlockVector3> {

    private final Iterable<? extends BlockVector3> iterable;
    private final MappedFaweQueue queue;

    public FastIterator(@Nonnull Iterable<? extends BlockVector3> iter, @Nullable EditSession extent) {
        this(iter, (HasFaweQueue) extent);
    }

    public FastIterator(@Nonnull Iterable<? extends BlockVector3> iter, @Nullable Extent extent) {
        this(iter, (HasFaweQueue) (extent != null ? (extent instanceof HasFaweQueue ? extent : new ExtentTraverser(extent).findAndGet(HasFaweQueue.class)) : null));
    }

    public FastIterator(@Nonnull Iterable<? extends BlockVector3> iter, @Nullable HasFaweQueue editSession) {
        this(iter, (FaweQueue) (editSession != null ? editSession.getQueue() : null));
    }

    public FastIterator(@Nonnull Iterable<? extends BlockVector3> iter, @Nullable FaweQueue faweQueue) {
        this.iterable = iter;
        this.queue = faweQueue != null && faweQueue instanceof MappedFaweQueue ? (MappedFaweQueue) faweQueue : null;
    }

    public Iterable<? extends BlockVector3> getIterable() {
        return iterable;
    }

    @Override
    public Iterator<BlockVector3> iterator() {
        if (queue == null || Settings.IMP.QUEUE.PRELOAD_CHUNKS <= 1) {
            return (Iterator<BlockVector3>) iterable.iterator();
        }
        return new Iterator<BlockVector3>() {
            Iterator<? extends BlockVector3> trailIter = iterable.iterator();
            Iterator<? extends BlockVector3> leadIter = iterable.iterator();
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
            public BlockVector3 next() {
                BlockVector3 pt = trailIter.next();
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
                                BlockVector3 v = leadIter.next();
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
