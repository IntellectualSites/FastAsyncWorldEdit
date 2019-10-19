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
 */
public class RegionVisitor implements Operation {

    public final Region region;
    public final RegionFunction function;
    public int affected = 0;
    public final Iterable<? extends BlockVector3> iterable;
    private final MappedFaweQueue queue;


    public RegionVisitor(Region region, RegionFunction function) {
        this(region, function, (FaweQueue) null);
    }

    public RegionVisitor(Region region, RegionFunction function, HasFaweQueue queue) {
        this((Iterable<BlockVector3>) region, function, queue);
    }

    public RegionVisitor(Iterable<? extends BlockVector3> iterable, RegionFunction function,
        HasFaweQueue hasQueue) {
        this.region = iterable instanceof Region ? (Region) iterable : null;
        this.function = function;
        this.iterable = iterable;
        this.queue = hasQueue != null && hasQueue.getQueue() instanceof MappedFaweQueue
            ? (MappedFaweQueue) hasQueue.getQueue() : null;
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
            /*
            Stopping the iteration only on an exception is stupid and can result in crashes. It
            should have never been considered. --MattBDev 2019-09-08
             */

            Iterator<? extends BlockVector3> trailingIterator = iterable.iterator();
            Iterator<? extends BlockVector3> leadingIterator = iterable.iterator();
            int lastTrailChunkX = Integer.MIN_VALUE;
            int lastTrailChunkZ = Integer.MIN_VALUE;
            int lastLeadChunkX = Integer.MIN_VALUE;
            int lastLeadChunkZ = Integer.MIN_VALUE;
            int loadingTarget = Settings.IMP.QUEUE.PRELOAD_CHUNKS;
                while (trailingIterator.hasNext()) {
                    BlockVector3 pt = trailingIterator.next();
                    this.apply(pt);
                    int chunkX = pt.getBlockX() >> 4;
                    int chunkZ = pt.getBlockZ() >> 4;
                    if (chunkX != lastTrailChunkX || chunkZ != lastTrailChunkZ) {
                        lastTrailChunkX = chunkX;
                        lastTrailChunkZ = chunkZ;
                        int amount;
                        if (lastLeadChunkX == Integer.MIN_VALUE) {
                            lastLeadChunkX = chunkX;
                            lastLeadChunkZ = chunkZ;
                            amount = loadingTarget;
                        } else {
                            amount = 1;
                        }
                        for (int count = 0; count < amount;) {
                            BlockVector3 v = leadingIterator.next();
                            int vcx = v.getBlockX() >> 4;
                            int vcz = v.getBlockZ() >> 4;
                            if (vcx != lastLeadChunkX || vcz != lastLeadChunkZ) {
                                lastLeadChunkX = vcx;
                                lastLeadChunkZ = vcz;
                                queue.queueChunkLoad(vcx, vcz);
                                count++;
                            }
                            // Skip the next 15 blocks
                            for (int x = 0; x <= 14; x++) {
                                leadingIterator.next();
                            }
                        }
                    }
                    for (int x = 0; x <= 14; x++) {
                        this.apply(trailingIterator.next());
                    }
                }
                while (trailingIterator.hasNext()) {
                    apply(trailingIterator.next());
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

