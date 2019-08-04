package com.boydti.fawe.beta;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import javax.annotation.Nullable;

public class SingleFilterBlock extends FilterBlock {

    private BaseBlock block;
    private int x, y, z;

    public SingleFilterBlock init(int x, int y, int z, BaseBlock block) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = block;
        return this;
    }

    @Override
    public Extent getExtent() {
        return this;
    }

    @Override
    public int getOrdinal() {
        return block.getOrdinal();
    }

    @Override
    public void setOrdinal(int ordinal) {
        setBlock(BlockState.getFromOrdinal(ordinal));
    }

    @Override
    public BlockState getBlock() {
        return block.toBlockState();
    }

    @Override
    public void setBlock(BlockState state) {
        setFullBlock(state.toBaseBlock(block.getNbtData()));
    }

    @Override
    public BaseBlock getFullBlock() {
        return block;
    }

//    @Override
//    public BaseBlock getFullBlockRelative(int x, int y, int z) {
//        return block;
//    }

    @Override
    public void setFullBlock(BaseBlock block) {
        this.block = block;
    }

    @Override
    public CompoundTag getNbtData() {
        return block.getNbtData();
    }

    @Override
    public void setNbtData(@Nullable CompoundTag nbtData) {
        block = block.toBaseBlock(nbtData);
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

    @Override
    public BlockVector3 getMinimumPoint() {
        return BlockVector3.at(x, y, z);
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return BlockVector3.at(x, y, z);
    }
}
