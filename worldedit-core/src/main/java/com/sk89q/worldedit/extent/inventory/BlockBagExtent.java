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

package com.sk89q.worldedit.extent.inventory;

import com.fastasyncworldedit.core.FaweCache;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Applies a {@link BlockBag} to operations.
 */
public class BlockBagExtent extends AbstractDelegateExtent {

    //FAWE start
    private final boolean mine;
    private final int[] missingBlocks = new int[BlockTypes.size()];
    //FAWE end
    private BlockBag blockBag;

    /**
     * Create a new instance.
     *
     * @param extent   the extent
     * @param blockBag the block bag
     */
    //FAWE start
    public BlockBagExtent(Extent extent, @Nullable BlockBag blockBag) {
        this(extent, blockBag, false);
    }

    public BlockBagExtent(Extent extent, @Nullable BlockBag blockBag, boolean mine) {
        super(extent);
        this.blockBag = blockBag;
        this.mine = mine;
    }
    //FAWe end

    /**
     * Get the block bag.
     *
     * @return a block bag, which may be null if none is used
     */
    @Nullable
    public BlockBag getBlockBag() {
        return blockBag;
    }

    /**
     * Set the block bag.
     *
     * @param blockBag a block bag, which may be null if none is used
     */
    public void setBlockBag(@Nullable BlockBag blockBag) {
        this.blockBag = blockBag;
    }

    /**
     * Gets the list of missing blocks and clears the list for the next
     * operation.
     *
     * @return a map of missing blocks
     */
    public Map<BlockType, Integer> popMissing() {
        //FAWE start - Use an Array
        HashMap<BlockType, Integer> map = new HashMap<>();
        for (int i = 0; i < missingBlocks.length; i++) {
            int count = missingBlocks[i];
            if (count > 0) {
                map.put(BlockTypes.get(i), count);
            }
        }
        Arrays.fill(missingBlocks, 0);
        return map;
        //FAWE end
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block) throws WorldEditException {
        return setBlock(position.getX(), position.getY(), position.getZ(), block);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) throws WorldEditException {
        BlockState existing = getBlock(x, y, z);
        if (!block.getBlockType().equals(existing.getBlockType())) {
            if (!block.getBlockType().getMaterial().isAir()) {
                try {
                    blockBag.fetchPlacedBlock(block.toImmutableState());
                } catch (UnplaceableBlockException e) {
                    throw FaweCache.BLOCK_BAG;
                } catch (BlockBagException e) {
                    //FAWE start - listen for internal ids
                    missingBlocks[block.getBlockType().getInternalId()]++;
                    throw FaweCache.BLOCK_BAG;
                    //FAWE end
                }
            }
            //FAWE start
            if (mine) {
                if (!existing.getBlockType().getMaterial().isAir()) {
                    try {
                        blockBag.storeDroppedBlock(existing);
                    } catch (BlockBagException ignored) {
                    }
                }
            }
            //FAWE end
        }

        return super.setBlock(x, y, z, block);
    }

}
