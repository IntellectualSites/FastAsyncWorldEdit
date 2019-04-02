package com.boydti.fawe.object.pattern;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;


import static com.google.common.base.Preconditions.checkNotNull;

public class DataPattern extends AbstractExtentPattern {
    private final Pattern pattern;

    public DataPattern(Extent extent, Pattern parent) {
        super(extent);
        checkNotNull(parent);
        this.pattern = parent;
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        BaseBlock oldBlock = getExtent().getFullBlock(position);
        BaseBlock newBlock = pattern.apply(position);
        return oldBlock.withPropertyId(newBlock.getInternalPropertiesId()).toBaseBlock();
    }
}
