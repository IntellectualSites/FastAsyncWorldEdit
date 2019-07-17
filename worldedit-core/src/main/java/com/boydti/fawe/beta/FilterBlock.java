package com.boydti.fawe.beta;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.blocks.TileEntityBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

import javax.annotation.Nullable;

import static com.sk89q.worldedit.world.block.BlockTypes.states;

public abstract class FilterBlock extends BlockVector3 implements Extent, TileEntityBlock {
    public abstract Extent getExtent();

    public abstract void setOrdinal(int ordinal);

    public abstract void setBlock(BlockState state);

    public abstract void setFullBlock(BaseBlock block);

    public void setBiome(BiomeType biome) {
        setBiome(getX(), getY(), getZ(), biome);
    }

    public abstract int getOrdinal();

    public abstract BlockState getBlock();

    public abstract BaseBlock getFullBlock();

    public abstract CompoundTag getNbtData();

    public abstract void setNbtData(@Nullable CompoundTag nbtData);

    public boolean hasNbtData() {
        return getNbtData() != null;
    }

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
        return getExtent().getBlock(x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return getExtent().getFullBlock(x, y, z);
    }

    public BlockState getBlockBelow() {
        return getBlock(getX(), getY() - 1, getZ());
    }

    public BlockState getBlockAbove() {
        return getBlock(getX(), getY() + 1, getZ());
    }

    public BlockState getBlockNorth() {
        return getBlock(getX(), getY(), getZ() - 1);
    }

    public BlockState getBlockEast() {
        return getBlock(getX() + 1, getY(), getZ());
    }

    public BlockState getBlockSouth() {
        return getBlock(getX(), getY(), getZ() + 1);
    }

    public BlockState getBlockWest() {
        return getBlock(getX() - 1, getY(), getZ());
    }

    public BlockState getBlockRelativeY(final int y) {
        return getBlock(getX(), getY() + y , getZ());
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

    /*
    Extent
     */
    public boolean setOrdinal(Extent orDefault, int ordinal) {
        setOrdinal(ordinal);
        return true;
    }

    public boolean setBlock(Extent orDefault, BlockState state) {
        setBlock(state);
        return true;
    }

    public boolean setFullBlock(Extent orDefault, BaseBlock block) {
        setFullBlock(block);
        return true;
    }

    public boolean setBiome(Extent orDefault, BiomeType biome) {
        setBiome(biome);
        return true;
    }

    public int getOrdinal(Extent orDefault) {
        return getOrdinal();
    }

    public BlockState getBlock(Extent orDefault) {
        return getBlock();
    }

    public BaseBlock getFullBlock(Extent orDefault) {
        return getFullBlock();
    }

    public CompoundTag getNbtData(Extent orDefault) {
        return getNbtData();
    }

    public BlockState getOrdinalBelow(Extent orDefault) {
        return getBlockBelow();
    }

    public BlockState getStateAbove(Extent orDefault) {
        return getBlockAbove();
    }

    public BlockState getStateRelativeY(Extent orDefault, final int y) {
        return getBlockRelativeY(y);
    }
}
