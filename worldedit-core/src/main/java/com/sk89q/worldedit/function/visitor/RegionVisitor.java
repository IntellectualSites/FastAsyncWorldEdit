/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.function.visitor;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.MappedFaweQueue;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.object.exception.FaweException;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import java.util.Iterator;
import java.util.List;

/**
 * Utility class to apply region functions to {@link com.sk89q.worldedit.regions.Region}.
 * @deprecated let the queue iterate, not the region function which lacks any kind of optimizations / parallelism
 */
@Deprecated
public class RegionVisitor implements Operation {

    public final Region region;
    public final RegionFunction function;
    public int affected = 0;
    public final Iterable<? extends BlockVector3> iterable;
    private final MappedFaweQueue queue;

    /**
     * Deprecated in favor of the other constructors which will preload chunks during iteration
     *
     * @param region
     * @param function
     */
    public RegionVisitor(Region region, RegionFunction function) {
        this(region, function, (FaweQueue) null);
    }

    public RegionVisitor(Region region, RegionFunction function, EditSession editSession) {
        this(region, function, editSession != null ? editSession.getQueue() : null);
    }

    public RegionVisitor(Region region, RegionFunction function, FaweQueue queue) {
        this((Iterable<BlockVector3>) region, function, queue);
    }

    public RegionVisitor(Iterable<? extends BlockVector3> iterable, RegionFunction function, HasFaweQueue hasQueue) {
        this.region = iterable instanceof Region ? (Region) iterable : null;
        this.function = function;
        this.iterable = iterable;
        this.queue = hasQueue != null && hasQueue.getQueue() instanceof MappedFaweQueue ? (MappedFaweQueue) hasQueue.getQueue() : null;
    }

    /**
     * Get the number of affected objects.
     *
     * @return the number of affected
     */
    public int getAffected() {
        return affected;
    }

    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        if (queue != null && Settings.IMP.QUEUE.PRELOAD_CHUNKS > 1) {
        	/*
             * The following is done to reduce iteration cost
             *  - Preload chunks just in time
             *  - Only check every 16th block for potential chunk loads
             *  - Stop iteration on exception instead of hasNext
             *  - Do not calculate the stacktrace as it is expensive
             */
            Iterator<? extends BlockVector3> trailIter = iterable.iterator();
            Iterator<? extends BlockVector3> leadIter = iterable.iterator();
            int lastTrailChunkX = Integer.MIN_VALUE;
            int lastTrailChunkZ = Integer.MIN_VALUE;
            int lastLeadChunkX = Integer.MIN_VALUE;
            int lastLeadChunkZ = Integer.MIN_VALUE;
            int loadingTarget = Settings.IMP.QUEUE.PRELOAD_CHUNKS;
            try {
                for (; ; ) {
                    BlockVector3 pt = trailIter.next();
                    apply(pt);
                    int cx = pt.getBlockX() >> 4;
                    int cz = pt.getBlockZ() >> 4;
                    if (cx != lastTrailChunkX || cz != lastTrailChunkZ) {
                        lastTrailChunkX = cx;
                        lastTrailChunkZ = cz;
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
                            // Skip the next 15 blocks
                            leadIter.next();
                            leadIter.next();
                            leadIter.next();
                            leadIter.next();
                            leadIter.next();
                            leadIter.next();
                            leadIter.next();
                            leadIter.next();
                            leadIter.next();
                            leadIter.next();
                            leadIter.next();
                            leadIter.next();
                            leadIter.next();
                            leadIter.next();
                            leadIter.next();
                        }
                    }
                    apply(trailIter.next());
                    apply(trailIter.next());
                    apply(trailIter.next());
                    apply(trailIter.next());
                    apply(trailIter.next());
                    apply(trailIter.next());
                    apply(trailIter.next());
                    apply(trailIter.next());
                    apply(trailIter.next());
                    apply(trailIter.next());
                    apply(trailIter.next());
                    apply(trailIter.next());
                    apply(trailIter.next());
                    apply(trailIter.next());
                    apply(trailIter.next());
                }
            } catch (FaweException e) {
                throw new RuntimeException(e);
            } catch (Throwable ignore) {
                ignore.printStackTrace();
            }
            try {
                while (true) {
                    apply(trailIter.next());
                    apply(trailIter.next());
                }
            } catch (FaweException e) {
                throw new RuntimeException(e);
            } catch (Throwable ignore) {
            }
        } else {
            for (BlockVector3 pt : iterable) {
                apply(pt);
            }
        }
        return null;
    }

    private void apply(BlockVector3 pt) throws WorldEditException {
        if (function.apply(pt)) {
            affected++;
        }
    }

    @Override
    public void cancel() {
    }

    @Override
    public void addStatusMessages(List<String> messages) {
        messages.add(BBC.VISITOR_BLOCK.format(getAffected()));
    }

}

