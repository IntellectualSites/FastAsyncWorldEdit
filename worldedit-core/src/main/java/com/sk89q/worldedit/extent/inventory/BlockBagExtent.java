package com.sk89q.worldedit.extent.inventory;

import com.boydti.fawe.object.exception.FaweException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Applies a {@link BlockBag} to operations.
 */
public class BlockBagExtent extends AbstractDelegateExtent {

    private final boolean mine;
    private int[] missingBlocks = new int[BlockTypes.size()];
    private BlockBag blockBag;

    /**
     * Create a new instance.
     *
     * @param extent   the extent
     * @param blockBag the block bag
     */
    public BlockBagExtent(Extent extent, @Nonnull BlockBag blockBag) {
        this(extent, blockBag, false);
    }

    public BlockBagExtent(Extent extent, @Nonnull BlockBag blockBag, boolean mine) {
        super(extent);
        checkNotNull(blockBag);
        this.blockBag = blockBag;
        this.mine = mine;
    }

    /**
     * Get the block bag.
     *
     * @return a block bag, which may be null if none is used
     */
    public
    @Nullable
    BlockBag getBlockBag() {
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
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 pos, B block) throws WorldEditException {
        final int x = pos.getBlockX();
        final int y = pos.getBlockY();
        final int z = pos.getBlockZ();
        if(blockBag != null) {
            BlockStateHolder lazyBlock = getExtent().getLazyBlock(x, y, z);
            BlockType fromType = lazyBlock.getBlockType();
            if(!block.getBlockType().equals(fromType)) {
                BlockType type = block.getBlockType();
                if (!type.getMaterial().isAir()) {
                    try {
                        blockBag.fetchPlacedBlock(block.toImmutableState());
                    } catch (UnplaceableBlockException e) {
                        throw new FaweException.FaweBlockBagException();
                    } catch (BlockBagException e) {
                        missingBlocks[type.getInternalId()]++;
                        throw new FaweException.FaweBlockBagException();
                    }
                }
                if (mine) {

                    if (!fromType.getMaterial().isAir()) {
                        try {
                            blockBag.storeDroppedBlock(fromType.getDefaultState());
                        } catch (BlockBagException ignored) {
                        }
                    }
                }
            }
        }
        return getExtent().setBlock(x, y, z, block);
    }

}
