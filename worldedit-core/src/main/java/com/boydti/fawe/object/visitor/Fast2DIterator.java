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

public class Fast2DIterator implements Iterable<BlockVector2> {

    private final Iterable<? extends BlockVector2> iterable;
    private final MappedFaweQueue queue;

    public Fast2DIterator(@Nonnull Iterable<? extends BlockVector2> iter, @Nullable EditSession extent) {
        this(iter, (HasFaweQueue) extent);
    }

    public Fast2DIterator(@Nonnull Iterable<? extends BlockVector2> iter, @Nullable Extent extent) {
        this(iter, (HasFaweQueue) (extent != null ? (extent instanceof HasFaweQueue ? extent : new ExtentTraverser(extent).findAndGet(HasFaweQueue.class)) : null));
    }

    public Fast2DIterator(@Nonnull Iterable<? extends BlockVector2> iter, @Nullable HasFaweQueue editSession) {
        this(iter, (FaweQueue) (editSession != null ? editSession.getQueue() : null));
    }

    public Fast2DIterator(@Nonnull Iterable<? extends BlockVector2> iter, @Nullable FaweQueue faweQueue) {
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
        return new Iterator<BlockVector2>() {
            Iterator<? extends BlockVector2> trailIter = iterable.iterator();
            Iterator<? extends BlockVector2> leadIter = iterable.iterator();
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
            public BlockVector2 next() {
                BlockVector2 pt = trailIter.next();
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
                                BlockVector2 v = leadIter.next();
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
