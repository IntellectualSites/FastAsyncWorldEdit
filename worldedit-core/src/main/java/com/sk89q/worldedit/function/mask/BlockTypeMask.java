package com.sk89q.worldedit.function.mask;

<<<<<<< HEAD
import com.sk89q.worldedit.Vector;
=======
import static com.google.common.base.Preconditions.checkNotNull;

>>>>>>> 399e0ad5... Refactor vector system to be cleaner
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class BlockTypeMask extends AbstractExtentMask {
    private final boolean[] types;

    protected BlockTypeMask(Extent extent, boolean[] types) {
        super(extent);
        this.types = types;
    }

    public BlockTypeMask(Extent extent, BlockType... types) {
        super(extent);
        this.types = new boolean[BlockTypes.size()];
        for (BlockType type : types) this.types[type.getInternalId()] = true;
    }

    /**
     * Create a new block mask.
     *
     * @param extent the extent
     * @param blocks a list of blocks to match
     */
    public BlockTypeMask(Extent extent, Collection<BlockType> blocks) {
        this(extent, blocks.toArray(new BlockType[blocks.size()]));
    }

    /**
     * Add the given blocks to the list of criteria.
     *
     * @param blocks a list of blocks
     */
    public void add(Collection<BlockType> blocks) {
        checkNotNull(blocks);
        for (BlockType type : blocks) {
            add(type);
        }
        for (BlockType type : blocks) {
            this.types[type.getInternalId()] = true;
        }
    }

    /**
     * Add the given blocks to the list of criteria.
     *
     * @param blocks an array of blocks
     */
    public void add(BlockType... blocks) {
        for (BlockType type : blocks) {
            this.types[type.getInternalId()] = true;
        }
    }

    /**
     * Get the list of blocks that are tested with.
     *
     * @return a list of blocks
     */
    public Collection<BlockType> getBlocks() {
        Set<BlockType> blocks = new HashSet<>();
        for (int i = 0; i < types.length; i++) {
            if (types[i]) blocks.add(BlockTypes.get(i));
        }
        return blocks;
    }

    @Override
<<<<<<< HEAD
    public boolean test(Vector vector) {
        return types[getExtent().getBlockType(vector).getInternalId()];
=======
    public boolean test(BlockVector3 vector) {
        return blocks.contains(getExtent().getBlock(vector).getBlockType());
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
>>>>>>> 399e0ad5... Refactor vector system to be cleaner
    }
}
