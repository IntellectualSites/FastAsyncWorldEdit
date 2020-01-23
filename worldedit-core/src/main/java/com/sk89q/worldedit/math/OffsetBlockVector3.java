package com.sk89q.worldedit.math;

public class OffsetBlockVector3 extends DelegateBlockVector3 {
    private final BlockVector3 offset;

    public OffsetBlockVector3(BlockVector3 offset) {
        this.offset = offset;
    }

    @Override
    public int getX() {
        return super.getX() + offset.getX();
    }

    @Override
    public int getY() {
        return super.getY() + offset.getY();
    }

    @Override
    public int getZ() {
        return super.getZ() + offset.getZ();
    }
}
