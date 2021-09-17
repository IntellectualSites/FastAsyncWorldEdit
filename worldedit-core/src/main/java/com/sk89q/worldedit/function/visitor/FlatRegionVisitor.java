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
import com.fastasyncworldedit.core.queue.implementation.ParallelQueueExtent;
import com.fastasyncworldedit.core.queue.implementation.SingleThreadQueueExtent;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.FlatRegionFunction;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.FlatRegion;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Applies region functions to columns in a {@link FlatRegion}.
 */
public class FlatRegionVisitor implements Operation {

    //FAWE start - chunk preloading
    private final SingleThreadQueueExtent singleQueue;
    private final FlatRegion flatRegion;
    //FAWE end
    private final FlatRegionFunction function;
    private int affected = 0;

    //FAWE start - chunk preloading

    /**
     * Create a new visitor.
     *
     * @param flatRegion a flat region
     * @param function   a function to apply to columns
     */
    public FlatRegionVisitor(FlatRegion flatRegion, FlatRegionFunction function) {
        this(flatRegion, function, null);
    }

    /**
     * Create a new visitor.
     *
     * @param flatRegion a flat region
     * @param function   a function to apply to columns
     * @param extent     the extent for preloading
     */
    public FlatRegionVisitor(FlatRegion flatRegion, FlatRegionFunction function, Extent extent) {
        checkNotNull(flatRegion);
        checkNotNull(function);

        this.function = function;
        this.flatRegion = flatRegion;
        if (extent != null) {
            ExtentTraverser<ParallelQueueExtent> queueTraverser = new ExtentTraverser<>(extent).find(ParallelQueueExtent.class);
            this.singleQueue = queueTraverser != null ? (SingleThreadQueueExtent) queueTraverser.get().getExtent() : null;
        } else {
            this.singleQueue = null;
        }
    }
    //FAWE end

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
        //FAWE start - chunk preloading
        if (singleQueue != null) {
            singleQueue.preload(flatRegion);
        }
        for (BlockVector2 pt : this.flatRegion.asFlatRegion()) {
            //FAWE end
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
    public Iterable<Component> getStatusMessages() {
        return ImmutableList.of(Caption.of(
                "worldedit.operation.affected.column",
                TextComponent.of(getAffected())
        ));
    }

}

