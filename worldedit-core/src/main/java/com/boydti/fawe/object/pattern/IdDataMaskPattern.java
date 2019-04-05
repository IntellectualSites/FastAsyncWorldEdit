package com.boydti.fawe.object.pattern;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

public class IdDataMaskPattern extends AbstractExtentPattern {
    private final Pattern pattern;
    private final int bitMask;

    public IdDataMaskPattern(Extent extent, Pattern parent, int bitMask) {
        super(extent);
        this.pattern = parent;
        this.bitMask = bitMask;
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        BaseBlock oldBlock = getExtent().getFullBlock(position);
        BaseBlock newBlock = pattern.apply(position);
        int oldData = oldBlock.getInternalPropertiesId();
        int newData = newBlock.getInternalPropertiesId() + oldData - (oldData & bitMask);
        return newBlock.withPropertyId(newData).toBaseBlock();
    }
}