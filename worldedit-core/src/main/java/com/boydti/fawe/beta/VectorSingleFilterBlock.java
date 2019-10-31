package com.boydti.fawe.beta;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

public class VectorSingleFilterBlock extends AbstractSingleFilterBlock {
    private final BlockVector3 mutable;

    public VectorSingleFilterBlock(BlockVector3 mutable) {
        this.mutable = mutable;
    }

    public VectorSingleFilterBlock init(BaseBlock block) {
        super.init(block);
        return this;
    }

    @Override
    public int getX() {
        return mutable.getX();
    }

    @Override
    public int getY() {
        return mutable.getY();
    }

    @Override
    public int getZ() {
        return mutable.getZ();
    }
}
