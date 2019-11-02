package com.boydti.fawe.beta.implementation.filter.block;

import com.sk89q.worldedit.world.block.BaseBlock;

public class SingleFilterBlock extends AbstractSingleFilterBlock {

    private int x, y, z;

    public SingleFilterBlock init(int x, int y, int z, BaseBlock block) {
        this.x = x;
        this.y = y;
        this.z = z;
        super.init(block);
        return this;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getZ() {
        return z;
    }
}
