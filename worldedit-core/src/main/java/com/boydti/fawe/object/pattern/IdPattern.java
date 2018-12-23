package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import static com.google.common.base.Preconditions.checkNotNull;

public class IdPattern extends AbstractExtentPattern {
    private final Pattern pattern;

    public IdPattern(Extent extent, Pattern parent) {
        super(extent);
        checkNotNull(parent);
        this.pattern = parent;
    }

    @Override
    public BlockStateHolder apply(BlockVector3 position) {
        BlockStateHolder oldBlock = getExtent().getBlock(position);
        BlockStateHolder newBlock = pattern.apply(position);
        return newBlock.withPropertyId(oldBlock.getInternalPropertiesId());
    }
}