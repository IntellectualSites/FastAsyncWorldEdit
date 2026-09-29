package com.fastasyncworldedit.core.extent.filter.block;

import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.TileEntityBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

import javax.annotation.Nullable;

/**
 * A class representing a block with a position
 * - Used for optimized block operations to avoid lookups
 */
public abstract class FilterBlock extends BlockVector3 implements Extent, TileEntityBlock {

    public abstract Extent getExtent();

    public abstract int getOrdinal();

    public abstract void setOrdinal(int ordinal);

    public abstract BlockState getBlock();

    public abstract void setBlock(BlockState state);

    public abstract BaseBlock getFullBlock();

    public abstract void setFullBlock(BaseBlock block);

    public abstract void setBiome(BiomeType biome);

    public abstract BiomeType getBiome();

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
    public boolean tile(final int x, final int y, final int z, final FaweCompoundTag tile) throws WorldEditException {
        return getExtent().tile(x, y, z, tile);
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return getExtent().getFullBlock(x, y, z);
    }

    public BlockState getBlockBelow() {
        return getBlock(x(), y() - 1, z());
    }

    public BlockState getBlockAbove() {
        return getBlock(x(), y() + 1, z());
    }

    public BlockState getBlockNorth() {
        return getBlock(x(), y(), z() - 1);
    }

    public BlockState getBlockEast() {
        return getBlock(x() + 1, y(), z());
    }

    public BlockState getBlockSouth() {
        return getBlock(x(), y(), z() + 1);
    }

    public BlockState getBlockWest() {
        return getBlock(x() - 1, y(), z());
    }

    public BlockState getBlockRelativeY(int y) {
        return getBlock(x(), y() + y, z());
    }

    @Override
    public abstract int x();

    @Override
    public abstract int y();

    @Override
    public abstract int z();

    public int getLocalX() {
        return x() & 15;
    }

    public int getLocalY() {
        return y() & 15;
    }

    public int getLocalZ() {
        return z() & 15;
    }

    public int getChunkX() {
        return x() >> 4;
    }

    public int getChunkZ() {
        return z() >> 4;
    }

    /*
    Extent
     */
    @Override
    public boolean setOrdinal(Extent orDefault, int ordinal) {
        setOrdinal(ordinal);
        return true;
    }

    @Override
    public boolean setBlock(Extent orDefault, BlockState state) {
        setBlock(state);
        return true;
    }

    @Override
    public boolean setFullBlock(Extent orDefault, BaseBlock block) {
        setFullBlock(block);
        return true;
    }

    @Override
    public int getOrdinal(Extent orDefault) {
        return getOrdinal();
    }

    @Override
    public BlockState getBlock(Extent orDefault) {
        return getBlock();
    }

    @Override
    public BaseBlock getFullBlock(Extent orDefault) {
        return getFullBlock();
    }

    @Override
    public boolean setBiome(Extent orDefault, BiomeType type) {
        setBiome(type);
        return true;
    }

    @Override
    public BiomeType getBiome(Extent orDefault) {
        return getBiome();
    }

    @Override
    public CompoundTag getNbtData(Extent orDefault) {
        return getNbtData();
    }

    @Override
    public BlockState getOrdinalBelow(Extent orDefault) {
        return getBlockBelow();
    }

    @Override
    public BlockState getStateAbove(Extent orDefault) {
        return getBlockAbove();
    }

    @Override
    public BlockState getStateRelativeY(Extent orDefault, int y) {
        return getBlockRelativeY(y);
    }

    @Override
    @Nullable
    public BiomeType getBiome(BlockVector3 position) {
        return null;
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        return setBiome(position.x(), position.y(), position.z(), biome);
    }

}
