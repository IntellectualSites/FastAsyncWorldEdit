package com.boydti.fawe.beta.implementation.filter.block;

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
 *  - Used for optimized block operations to avoid lookups
 */
public abstract class FilterBlock extends BlockVector3 implements Extent, TileEntityBlock {

    public abstract Extent getExtent();

    public void setBiome(BiomeType biome) {
        setBiome(getX(), getY(), getZ(), biome);
    }

    public abstract int getOrdinal();

    public abstract void setOrdinal(int ordinal);

    public abstract BlockState getBlock();

    public abstract void setBlock(BlockState state);

    public abstract BaseBlock getFullBlock();

    public abstract void setFullBlock(BaseBlock block);

    @Override
    public abstract CompoundTag getNbtData();

    @Override
    public abstract void setNbtData(@Nullable CompoundTag nbtData);

    @Override
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
    public boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        return getExtent().setTile(x, y, z, tile);
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

    public BlockState getBlockRelativeY(int y) {
        return getBlock(getX(), getY() + y, getZ());
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

    @Override @Nullable
    public BiomeType getBiome(BlockVector3 position) {
        return null;
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        return setBiome(position.getX(), position.getY(), position.getZ(), biome);
    }
}
