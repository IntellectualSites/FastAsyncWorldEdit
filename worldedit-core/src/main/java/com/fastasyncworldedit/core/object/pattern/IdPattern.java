package com.fastasyncworldedit.core.object.pattern;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

import static com.google.common.base.Preconditions.checkNotNull;

public class IdPattern extends AbstractExtentPattern {
    private final Pattern pattern;

    public IdPattern(Extent extent, Pattern parent) {
        super(extent);
        checkNotNull(parent);
        this.pattern = parent;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        BaseBlock oldBlock = getExtent().getFullBlock(position);
        BaseBlock newBlock = pattern.applyBlock(position);
        return newBlock.withPropertyId(oldBlock.getInternalPropertiesId()).toBaseBlock();
    }
}
