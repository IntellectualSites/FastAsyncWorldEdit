package com.boydti.fawe.beta;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

import javax.annotation.Nullable;

public abstract class FilterBlock extends BlockVector3 implements Extent {
    private final Extent extent;

    public FilterBlock(Extent extent) {
        this.extent = extent;
    }

    public final Extent getExtent() {
        return extent;
    }

    public abstract void setOrdinal(int ordinal);

    public abstract void setState(BlockState state);

    public abstract void setFullBlock(BaseBlock block);

    public abstract int getOrdinal();

    public abstract BlockState getState();

    public abstract BaseBlock getBaseBlock();

    public abstract CompoundTag getTag();

    @Override
    public BlockVector3 getMinimumPoint() {
        return getExtent().getMinimumPoint();
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return getExtent().getMaximumPoint();
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return getStateRelative(x - getX(), y - getY(), z - getZ());
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return getFullBlockRelative(x - getX(), y - getY(), z - getZ());
    }

    public BlockState getOrdinalBelow() {
        return getStateRelative(0, -1, 0);
    }

    public BlockState getStateAbove() {
        return getStateRelative(0, 1, 0);
    }

    public BlockState getStateRelativeY(final int y) {
        return getStateRelative(0, y, 0);
    }

    public BlockState getStateRelative(final int x, final int y, final int z) {
        return getFullBlockRelative(x, y, z).toBlockState();
    }

    public BaseBlock getFullBlockRelative(int x, int y, int z) {
        return getExtent().getFullBlock(x + getX(), y + getY(), z + getZ());
    }

    @Override
    public abstract int getX();

    @Override
    public abstract int getY();

    @Override
    public abstract int getZ();

    public int getLocalX() {
        return getX() & 15;
    }

    public int getLocalY() {
        return getY() & 15;
    }

    public int getLocalZ() {
        return getZ() & 15;
    }

    public int getChunkX() {
        return getX() >> 4;
    }

    public int getChunkZ() {
        return getZ() >> 4;
    }
}
