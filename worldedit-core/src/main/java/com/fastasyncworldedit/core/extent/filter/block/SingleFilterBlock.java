package com.fastasyncworldedit.core.extent.filter.block;

import com.sk89q.worldedit.world.block.BaseBlock;

public class SingleFilterBlock extends AbstractSingleFilterBlock {

    private int x;
    private int y;
    private int z;

    public SingleFilterBlock init(int x, int y, int z, BaseBlock block) {
        this.x = x;
        this.y = y;
        this.z = z;
        super.init(block);
        return this;
    }

    @Override
    public int x() {
        return x;
    }

    @Override
    public int y() {
        return y;
    }

    @Override
    public int z() {
        return z;
    }

}
