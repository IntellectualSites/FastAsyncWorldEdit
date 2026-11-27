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

package com.sk89q.worldedit.function.block;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.LayerFunction;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

public class Thawer implements LayerFunction {

    private final BlockState air = BlockTypes.AIR.getDefaultState();
    private final BlockState water = BlockTypes.WATER.getDefaultState();

    private final Extent extent;

    private int affected;

    //FAWE start - avoid reading the ground block twice
    private BlockVector3 lastPosition;
    private BlockState lastBlock;
    //FAWE end

    /**
     * Create a new instance.
     *
     * @param extent the extent to thaw in
     */
    public Thawer(Extent extent) {
        this.extent = extent;
        this.affected = 0;
    }

    public int getAffected() {
        return this.affected;
    }

    @Override
    public boolean isGround(BlockVector3 position) {
        BlockState block = this.extent.getBlock(position);
        //FAWE start - avoid reading the ground block twice
        this.lastPosition = position;
        this.lastBlock = block;
        //FAWE end

        // Stop searching when we hit anything non-air
        return !block.getBlockType().getMaterial().isAir();
    }

    @Override
    public boolean apply(BlockVector3 position, int depth) throws WorldEditException {
        if (depth > 0) {
            // We only care about the first layer.
            return false;
        }

        //FAWE start - avoid reading the ground block twice
        // LayerVisitor passes the instance isGround accepted straight to apply, with no edit in between
        BlockState block = position == this.lastPosition ? this.lastBlock : this.extent.getBlock(position);
        //FAWE end
        BlockType blockType = block.getBlockType();

        if (blockType == BlockTypes.ICE) {
            if (this.extent.setBlock(position, water)) {
                affected++;
            }
        } else if (blockType == BlockTypes.SNOW) {
            if (this.extent.setBlock(position, air)) {
                affected++;
                //FAWE start - the block beneath is no longer snowy
                resetSnowy(position);
                //FAWE end
            }
        }

        return false;
    }

    //FAWE start - the block beneath is no longer snowy
    private void resetSnowy(BlockVector3 snowPosition) throws WorldEditException {
        if (snowPosition.y() <= this.extent.getMinimumPoint().y()) {
            return;
        }
        BlockVector3 belowPosition = snowPosition.add(0, -1, 0);
        BlockState below = this.extent.getBlock(belowPosition);
        if (below.getBlockType().hasProperty(SnowSimulator.SNOWY)
                && this.extent.setBlock(belowPosition, below.with(SnowSimulator.SNOWY, false))) {
            affected++;
        }
    }
    //FAWE end

}
