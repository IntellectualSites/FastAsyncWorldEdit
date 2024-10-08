package com.fastasyncworldedit.core.history.changeset;

import com.fastasyncworldedit.core.FaweCache;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.inventory.BlockBagException;
import com.sk89q.worldedit.extent.inventory.UnplaceableBlockException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BlockBagChangeSet extends AbstractDelegateChangeSet {

    private final boolean mine;
    private final int[] missingBlocks = new int[BlockTypes.size()];
    private BlockBag blockBag;

    public BlockBagChangeSet(AbstractChangeSet parent, BlockBag blockBag, boolean mine) {
        super(parent);
        this.blockBag = blockBag;
        this.mine = mine;
    }

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
        HashMap<BlockType, Integer> map = new HashMap<>();
        for (int i = 0; i < missingBlocks.length; i++) {
            int count = missingBlocks[i];
            if (count > 0) {
                map.put(BlockTypes.get(i), count);
            }
        }
        Arrays.fill(missingBlocks, 0);
        return map;
    }

    @Override
    public void add(BlockVector3 loc, BaseBlock from, BaseBlock to) {
        int x = loc.x();
        int y = loc.y();
        int z = loc.z();
        add(x, y, z, from, to);
    }

    @Override
    public void add(int x, int y, int z, BaseBlock from, BaseBlock to) {
        check(from.getBlockType(), to.getBlockType());
        super.add(x, y, z, from, to);
    }

    public void check(BlockType typeFrom, BlockType typeTo) {
        if (!typeTo.getMaterial().isAir()) {
            try {
                blockBag.fetchPlacedBlock(typeTo.getDefaultState());
            } catch (UnplaceableBlockException e) {
                throw FaweCache.BLOCK_BAG;
            } catch (BlockBagException e) {
                missingBlocks[typeTo.getInternalId()]++;
                throw FaweCache.BLOCK_BAG;
            }
        }
        if (mine) {
            if (!typeFrom.getMaterial().isAir()) {
                try {
                    blockBag.storeDroppedBlock(typeFrom.getDefaultState());
                } catch (BlockBagException ignored) {
                }
            }
        }
    }

    @Override
    public void add(int x, int y, int z, int combinedFrom, int combinedTo) {
        BlockType typeFrom = BlockTypes.getFromStateId(combinedFrom);
        BlockType typeTo = BlockTypes.getFromStateId(combinedTo);
        check(typeFrom, typeTo);
        super.add(x, y, z, combinedFrom, combinedTo);
    }

}
