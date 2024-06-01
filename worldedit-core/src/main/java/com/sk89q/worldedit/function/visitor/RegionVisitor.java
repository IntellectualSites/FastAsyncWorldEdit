/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.function.visitor;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.fastasyncworldedit.core.queue.implementation.ParallelQueueExtent;
import com.fastasyncworldedit.core.queue.implementation.SingleThreadQueueExtent;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import java.util.Iterator;

/**
 * Utility class to apply region functions to {@link com.sk89q.worldedit.regions.Region}.
 *
 * @deprecated - FAWE deprecation: Let the queue iterate, not the region function which lacks any kind of optimizations / parallelism
 */
@Deprecated
public class RegionVisitor implements Operation {

    public final Iterable<? extends BlockVector3> iterable;
    //FAWE start - allow chunk preloading
    private final SingleThreadQueueExtent singleQueue;
    //FAWE end
    private final Region region;
    private final RegionFunction function;
    private int affected = 0;

    /**
     * @deprecated Use other constructors which will preload chunks during iteration
     */
    @Deprecated
    public RegionVisitor(Region region, RegionFunction function) {
        this(region, function, null);
    }

    /**
     * Allows for preloading chunks, and non-specific "regions" to be visited.
     *
     * @param iterable Can be supplied as a region, or a raw iterator
     * @param function The function to be applied to each BlockVector3 iterated over
     * @param extent   Supplied editsession/extent to attempt to extract {@link SingleThreadQueueExtent} from
     */
    public RegionVisitor(Iterable<BlockVector3> iterable, RegionFunction function, Extent extent) {
        region = iterable instanceof Region ? (Region) iterable : null;
        this.iterable = iterable;
        this.function = function;
        if (extent != null) {
            ExtentTraverser<ParallelQueueExtent> queueTraverser = new ExtentTraverser<>(extent).find(ParallelQueueExtent.class);
            this.singleQueue = queueTraverser != null ? (SingleThreadQueueExtent) queueTraverser.get().getExtent() : null;
        } else {
            this.singleQueue = null;
        }
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
        //FAWE start > allow chunk preloading
        if (singleQueue != null && Settings.settings().QUEUE.PRELOAD_CHUNK_COUNT > 1) {
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
            int loadingTarget = Settings.settings().QUEUE.PRELOAD_CHUNK_COUNT;
            while (trailIter.hasNext()) {
                BlockVector3 pt = trailIter.next();
                apply(pt);
                int cx = pt.x() >> 4;
                int cz = pt.z() >> 4;
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
                    try {
                        lead:
                        for (int count = 0; count < amount; ) {
                            BlockVector3 v = leadIter.next();
                            int vcx = v.x() >> 4;
                            int vcz = v.z() >> 4;
                            if (vcx != lastLeadChunkX || vcz != lastLeadChunkZ) {
                                lastLeadChunkX = vcx;
                                lastLeadChunkZ = vcz;
                                singleQueue.addChunkLoad(vcx, vcz);
                                count++;
                            }
                            // Skip the next 15 blocks
                            for (int i = 0; i < 16; i++) {
                                if (!leadIter.hasNext()) {
                                    break lead;
                                }
                                leadIter.next();
                            }
                        }
                    } catch (FaweException e) {
                        // Likely to be a low memory or cancellation exception.
                        throw new RuntimeException(e);
                    } catch (Throwable ignored) {
                        // Ignore as it is likely not something too important, and we can continue with the operation
                    }
                }
                for (int i = 0; i < 16; i++) {
                    if (!trailIter.hasNext()) {
                        return null;
                    }
                    apply(trailIter.next());
                }
            }
        } else {
            for (BlockVector3 pt : region) {
                apply(pt);
            }
        }
        //FAWE end
        return null;
    }

    //FAWE start > extract methods for slightly clean resume method
    private void apply(BlockVector3 pt) throws WorldEditException {
        if (function.apply(pt)) {
            affected++;
        }
    }
    //FAWE end

    @Override
    public void cancel() {
    }

    @Override
    public Iterable<Component> getStatusMessages() {
        return ImmutableList.of(Caption.of("worldedit.operation.affected.block", TextComponent.of(getAffected())));
    }

}

