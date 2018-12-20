package com.boydti.fawe.object.pattern;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

public class IdDataMaskPattern extends AbstractExtentPattern {
    private final Pattern pattern;
    private final int bitMask;
    private final BaseBlock mutable = new BaseBlock(BlockTypes.AIR);

    public IdDataMaskPattern(Extent extent, Pattern parent, int bitMask) {
        super(extent);
        this.pattern = parent;
        this.bitMask = bitMask;
    }

    @Override
    public BlockStateHolder apply(Vector position) {
        BlockStateHolder oldBlock = getExtent().getBlock(position);
        BlockStateHolder newBlock = pattern.apply(position);
        int oldData = oldBlock.getInternalPropertiesId();
        int newData = newBlock.getInternalPropertiesId() + oldData - (oldData & bitMask);
        return newBlock.withPropertyId(newData);
    }
}