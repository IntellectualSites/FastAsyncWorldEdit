package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

public class SingleBlockTypeMask extends AbstractExtentMask {
    private final int internalId;

    public SingleBlockTypeMask(Extent extent, BlockType type) {
        super(extent);
        this.internalId = type.getInternalId();
    }

    @Override
    public boolean test(Vector vector) {
        return getExtent().getBlockType(vector).getInternalId() == internalId;
    }

    @Override
    public Mask inverse() {
        return new BlockMaskBuilder().add(BlockTypes.get(internalId)).build(getExtent()).inverse();
    }

    public BlockType getBlockType() {
        return BlockTypes.get(internalId);
    }
}
