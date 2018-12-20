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
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.regions.Region;
import java.util.Iterator;
import java.util.List;

/**
 * Utility class to apply region functions to {@link com.sk89q.worldedit.regions.Region}.
 */
public class RegionVisitor implements Operation {

    public final Region region;
    public final Iterable<? extends Vector> iterable;
    public final RegionFunction function;
    private final MappedFaweQueue queue;
    private boolean useCuboidIterator = false;
    public int affected = 0;

    /**
     * Deprecated in favor of the other constructors which will preload chunks during iteration
     *
     * @param region
     * @param function
     */
    @Deprecated
    public RegionVisitor(Region region, RegionFunction function) {
        this(region, function, (FaweQueue) null);
    }

    public RegionVisitor(Region region, RegionFunction function, EditSession editSession) {
        this(region, function, editSession != null ? editSession.getQueue() : null);
    }

    public RegionVisitor(Region region, RegionFunction function, FaweQueue queue) {
        this((Iterable<BlockVector>) region, function, queue);
    }

    public RegionVisitor(Iterable<? extends Vector> iterable, RegionFunction function, HasFaweQueue hasQueue) {
        region = (iterable instanceof Region) ? (Region) iterable : null;
        this.iterable = iterable;
        this.function = function;
        this.queue = hasQueue != null && hasQueue.getQueue() instanceof MappedFaweQueue ? (MappedFaweQueue) hasQueue.getQueue() : null;
    }

    /**
     * Get the number of affected objects.
     *
     * @return the number of affected
     */
    public int getAffected() {
        return this.affected;
    }

    @Override
    public Operation resume(final RunContext run) throws WorldEditException {
        if (queue != null && Settings.IMP.QUEUE.PRELOAD_CHUNKS > 1) {
            /*
             * The following is done to reduce iteration cost
             *  - Preload chunks just in time
             *  - Only check every 16th block for potential chunk loads
             *  - Stop iteration on exception instead of hasNext
             *  - Do not calculate the stacktrace as it is expensive
             */
            Iterator<? extends Vector> trailIter = iterable.iterator();
            Iterator<? extends Vector> leadIter = iterable.iterator();
            int lastTrailChunkX = Integer.MIN_VALUE;
            int lastTrailChunkZ = Integer.MIN_VALUE;
            int lastLeadChunkX = Integer.MIN_VALUE;
            int lastLeadChunkZ = Integer.MIN_VALUE;
            int loadingTarget = Settings.IMP.QUEUE.PRELOAD_CHUNKS;
            try {
                for (; ; ) {
                    Vector pt = trailIter.next();
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
                            Vector v = leadIter.next();
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
            }
            try {
                for (; ; ) {
                    apply(trailIter.next());
                    apply(trailIter.next());
                }
            } catch (FaweException e) {
                throw new RuntimeException(e);
            } catch (Throwable ignore) {
            }
        } else {
            for (Vector pt : iterable) {
                apply(pt);
            }
        }
        return null;
    }

    private void apply(Vector pt) throws WorldEditException {
        if (function.apply(pt)) {
            affected++;
        }
    }

    @Override
    public void cancel() {
    }

    @Override
    public void addStatusMessages(final List<String> messages) {
        messages.add(BBC.VISITOR_BLOCK.format(getAffected()));
    }


}
