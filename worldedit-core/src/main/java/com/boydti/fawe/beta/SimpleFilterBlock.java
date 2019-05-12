package com.boydti.fawe.beta;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nullable;

public abstract class SimpleFilterBlock extends FilterBlock {
    public SimpleFilterBlock(Extent extent) {
        super(extent);
    }

    private int x, y, z, ordinal;
    private CompoundTag nbt;

    public void init(int x, int y, int z, int ordinal) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.ordinal = ordinal;
    }

    public void init(int x, int y, int z, int ordinal, CompoundTag nbt) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.ordinal = ordinal;
        this.nbt = nbt;
    }

    @Override
    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public BlockState getState() {
        return BlockTypes.states[ordinal];
    }

    @Override
    public BaseBlock getBaseBlock() {
        return getState().toBaseBlock(nbt);
    }

    @Override
    public CompoundTag getTag() {
        return nbt;
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
