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
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

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

    /**
     * Deprecated in favor of the other constructors which will preload chunks during iteration
     *
     * @param region
     * @param function
     */
    public RegionVisitor(Region region, RegionFunction function) {
        this((Iterable<BlockVector3>) region, function);
    }

    @Deprecated
    public RegionVisitor(Iterable<BlockVector3> iterable, RegionFunction function) {
        this.region = iterable instanceof Region ? (Region) iterable : null;
        this.function = function;
        this.iterable = iterable;
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
        for (BlockVector3 pt : iterable) {
            if (function.apply(pt)) {
                affected++;
            }
        }
        return null;
    }

    @Override
    public void cancel() {
    }

    @Override
    public void addStatusMessages(List<String> messages) {
        messages.add(BBC.VISITOR_BLOCK.format(getAffected()));
    }

}

