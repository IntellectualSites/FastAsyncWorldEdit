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
import com.sk89q.worldedit.registry.state.BooleanProperty;
import com.sk89q.worldedit.registry.state.EnumProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockCategories;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Comparator;

//FAWE start - rewrite simulator
public class SnowSimulator implements LayerFunction {

    public static final BooleanProperty SNOWY = (BooleanProperty) (Property<?>) BlockTypes.GRASS_BLOCK.getProperty("snowy");
    private static final EnumProperty PROPERTY_SLAB = (EnumProperty) (Property<?>) BlockTypes.OAK_SLAB.getProperty("type");
    private static final EnumProperty PROPERTY_STAIR = (EnumProperty) (Property<?>) BlockTypes.OAK_STAIRS.getProperty("half");
    private static final EnumProperty PROPERTY_TRAPDOOR = (EnumProperty) (Property<?>) BlockTypes.OAK_TRAPDOOR.getProperty("half");
    private static final BooleanProperty PROPERTY_TRAPDOOR_OPEN = (BooleanProperty) (Property<?>) BlockTypes.OAK_TRAPDOOR.getProperty("open");

    private static final BlockState ICE = BlockTypes.ICE.getDefaultState();
    private static final BlockState SNOW = BlockTypes.SNOW.getDefaultState();
    private static final BlockState SNOW_BLOCK = BlockTypes.SNOW_BLOCK.getDefaultState();

    private static final Property<Integer> PROPERTY_SNOW_LAYERS = BlockTypes.SNOW.getProperty("layers");
    private static final Property<Integer> PROPERTY_WATER_LEVEL = BlockTypes.WATER.getProperty("level");

    private static final String PROPERTY_VALUE_TOP = "top";
    private static final String PROPERTY_VALUE_BOTTOM = "bottom";
    private static final int MAX_SNOW_LAYER = PROPERTY_SNOW_LAYERS.getValues().stream().max(Comparator.naturalOrder()).orElse(8);

    private final Extent extent;
    private final boolean stack;
    private int affected;

    public SnowSimulator(Extent extent, boolean stack) {
        this.extent = extent;
        this.stack = stack;
        this.affected = 0;
    }

    public int getAffected() {
        return this.affected;
    }

    @Override
    public boolean isGround(BlockVector3 position) {
        final BlockState block = this.extent.getBlock(position);
        final BlockType blockType = block.getBlockType();

        // We're returning the first block we can (potentially) place *on top of*
        if (blockType.getMaterial().isAir() || (stack && blockType == BlockTypes.SNOW)) {
            return false;
        }

        // Unless it's water
        if (blockType == BlockTypes.WATER) {
            return true;
        }

        // Stop searching when we hit a movement blocker
        return blockType.getMaterial().isMovementBlocker();
    }

    @Override
    public boolean apply(BlockVector3 position, int depth) throws WorldEditException {
        if (depth > 0) {
            // We only care about the first layer.
            return false;
        }

        final BlockState block = this.extent.getBlock(position);
        final BlockType blockType = block.getBlockType();

        // If affected block is water, replace with ice
        if (blockType == BlockTypes.WATER) {
            if (shouldFreeze(position, block) && this.extent.setBlock(position.x(), position.y(), position.z(), ICE)) {
                affected++;
            }
            return false;
        }

        // Can't put snow this far up
        if (position.y() == this.extent.getMaximumPoint().y()) {
            return false;
        }

        BlockVector3 abovePosition = position.add(0, 1, 0);
        BlockState above = this.extent.getBlock(abovePosition);

        if (!shouldSnow(block, above)) {
            return false;
        }

        // in stack mode, we want to increase existing snow layers
        if (stack && above.getBlockType() == BlockTypes.SNOW) {
            int layers = above.getState(PROPERTY_SNOW_LAYERS);
            // if we would place the last possible layer (in current versions layer 8) we just replace with a snow block and
            // set the block beneath snowy (if property is applicable, example would be grass with snow texture on top)
            if (layers == MAX_SNOW_LAYER - 1 && !this.extent.setBlock(abovePosition, SNOW_BLOCK)) {
                return false;
            }
            // we've not reached the top snow layer yet, so just add another layer
            if (!this.extent.setBlock(abovePosition, above.with(PROPERTY_SNOW_LAYERS, layers + 1))) {
                return false;
            }
        } else {
            if (!this.extent.setBlock(abovePosition, SNOW)) {
                return false;
            }
        }
        // set block beneath snow (layers) snowy, if applicable
        if (block.getBlockType().hasProperty(SNOWY)) {
            this.extent.setBlock(position, block.with(SNOWY, true));
        }
        this.affected++;
        return false;
    }

    /**
     * Check if snow should be placed at {@code above}
     *
     * @param blockState The block under the snow layer
     * @param above      The block which will hold the snow layer
     * @return if snow should be placed
     */
    private boolean shouldSnow(BlockState blockState, BlockState above) {
        // simplified net.minecraft.world.level.biome.Biome#shouldSnow
        // if the block, where the snow should be actually placed at, is not air or snow (if in stack mode), we can't place
        // anything
        if (!(above.isAir() || (above.getBlockType() == BlockTypes.SNOW && stack))) {
            return false;
        }
        // net.minecraft.world.level.block.SnowLayerBlock#canSurvive
        if (BlockCategories.SNOW_LAYER_CANNOT_SURVIVE_ON.contains(blockState)) {
            return false;
        }
        if (BlockCategories.SNOW_LAYER_CAN_SURVIVE_ON.contains(blockState)) {
            return true;
        }
        BlockType type = blockState.getBlockType();

        // net.minecraft.world.level.block.Block.isFaceFull (block has 1x1x1 bounding box)
        if (type.getMaterial().isFullCube()) {
            return true;
        }
        // if block beneath potential snow layer has snow layers, we can place snow if all possible layers are present.
        if (type == BlockTypes.SNOW && blockState.getState(PROPERTY_SNOW_LAYERS) == MAX_SNOW_LAYER) {
            return true;
        }
        // return potential non-full blocks, which could hold snow layers due to block states
        // if block is a slab, needs to be on the upper part of the block
        if (type.hasProperty(PROPERTY_SLAB)) {
            return PROPERTY_VALUE_TOP.equals(blockState.getState(PROPERTY_SLAB));
        }
        // if block is a trapdoor, the trapdoor must NOT be open
        if (type.hasProperty(PROPERTY_TRAPDOOR_OPEN) && blockState.getState(PROPERTY_TRAPDOOR_OPEN)) {
            return false;
        }
        // if block is a closed trapdoor, the trapdoor must be aligned at the top part of the block
        if (type.hasProperty(PROPERTY_TRAPDOOR)) {
            return PROPERTY_VALUE_TOP.equals(blockState.getState(PROPERTY_TRAPDOOR));
        }
        // if block is a stair, it must be "bottom" (upside-down)
        if (type.hasProperty(PROPERTY_STAIR)) {
            return PROPERTY_VALUE_BOTTOM.equals(blockState.getState(PROPERTY_STAIR));
        }
        return false;
    }

    // net.minecraft.world.level.biome.Biome#shouldFreeze
    private boolean shouldFreeze(BlockVector3 position, BlockState blockState) {
        return blockState.getBlockType() == BlockTypes.WATER &&
                blockState.getState(PROPERTY_WATER_LEVEL) == 0 &&
                this.extent.getEmittedLight(position) < 10;
    }

}
