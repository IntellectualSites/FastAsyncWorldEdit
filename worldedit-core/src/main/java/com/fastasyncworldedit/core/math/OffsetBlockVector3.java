package com.fastasyncworldedit.core.math;

import com.sk89q.worldedit.math.BlockVector3;

public class OffsetBlockVector3 extends DelegateBlockVector3 {

    private final BlockVector3 offset;

    public OffsetBlockVector3(BlockVector3 offset) {
        this.offset = offset;
    }

    @Override
    public int x() {
        return super.x() + offset.x();
    }

    @Override
    public int y() {
        return super.y() + offset.y();
    }

    @Override
    public int z() {
        return super.z() + offset.z();
    }

}
