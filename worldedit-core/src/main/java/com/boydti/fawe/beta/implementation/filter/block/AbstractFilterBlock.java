package com.boydti.fawe.beta.implementation.filter.block;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import javax.annotation.Nullable;

public abstract class AbstractFilterBlock extends FilterBlock {

    @Override
    public abstract BaseBlock getFullBlock();

    @Override
    public abstract void setFullBlock(BaseBlock block);

    public abstract BlockVector3 getPosition();

    @Override
    public abstract Extent getExtent();

    @Override
    public int getX() {
        return getPosition().getX();
    }

    @Override
    public int getY() {
        return getPosition().getY();
    }

    @Override
    public int getZ() {
        return getPosition().getZ();
    }

    @Override
    public int getOrdinal() {
        return getBlock().getOrdinal();
    }

    @Override
    public void setOrdinal(int ordinal) {
        setBlock(BlockState.getFromOrdinal(ordinal));
    }

    @Override
    public BlockState getBlock() {
        return getFullBlock().toBlockState();
    }

    @Override
    public void setBlock(BlockState state) {
        setFullBlock(state.toBaseBlock(getBlock().getNbtData()));
    }

    @Override
    public CompoundTag getNbtData() {
        return getFullBlock().getNbtData();
    }

    @Override
    public void setNbtData(@Nullable CompoundTag nbtData) {
        setFullBlock(getFullBlock().toBaseBlock(nbtData));
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return BlockVector3.at(getX(), getY(), getZ());
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return BlockVector3.at(getX(), getY(), getZ());
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block)
        throws WorldEditException {
        if (x == this.getX() && y == this.getY() && z == this.getZ()) {
            setFullBlock(block.toBaseBlock());
            return true;
        }
        return getExtent().setBlock(x, y, z, block);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return getExtent().setBiome(x, y, z, biome);
    }
}
