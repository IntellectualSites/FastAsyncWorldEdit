package com.boydti.fawe.beta;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

public class NorthVector extends BlockVector3 {

    private final BlockVector3 parent;

    public NorthVector(BlockVector3 parent) {
        this.parent = parent;
    }

//    @Override
//    public BlockVector3 south(BlockVector3 orDefault) {
//        return parent;
//    }

    @Override
    public int getX() {
        return parent.getX();
    }

    @Override
    public int getY() {
        return parent.getY();
    }

    @Override
    public int getZ() {
        return parent.getZ();
    }

    @Override
    public boolean setOrdinal(Extent orDefault, int ordinal) {
        return orDefault.setBlock(this, BlockState.getFromOrdinal(ordinal));
    }

    @Override
    public boolean setBlock(Extent orDefault, BlockState state) {
        return orDefault.setBlock(this, state);
    }

    @Override
    public boolean setFullBlock(Extent orDefault, BaseBlock block) {
        return orDefault.setBlock(this, block);
    }

    @Override
    public boolean setBiome(Extent orDefault, BiomeType biome) {
        return orDefault.setBiome(getX(), getY(), getZ(), biome);
    }

    @Override
    public int getOrdinal(Extent orDefault) {
        return getBlock(orDefault).getOrdinal();
    }

    @Override
    public char getOrdinalChar(Extent orDefault) {
        return (char) getOrdinal(orDefault);
    }

    @Override
    public BlockState getBlock(Extent orDefault) {
        return orDefault.getBlock(this);
    }

    @Override
    public BaseBlock getFullBlock(Extent orDefault) {
        return orDefault.getFullBlock(this);
    }

    @Override
    public CompoundTag getNbtData(Extent orDefault) {
        return orDefault.getFullBlock(getX(), getY(), getZ()).getNbtData();
    }

    @Override
    public BlockState getOrdinalBelow(Extent orDefault) {
        return getStateRelative(orDefault, 0, -1, 0);
    }

    @Override
    public BlockState getStateAbove(Extent orDefault) {
        return getStateRelative(orDefault, 0, 1, 0);
    }

    @Override
    public BlockState getStateRelativeY(Extent orDefault, int y) {
        return getStateRelative(orDefault, 0, y, 0);
    }

    public BlockState getStateRelative(Extent orDefault, int x, int y, int z) {
        return getFullBlockRelative(orDefault, x, y, z).toBlockState();
    }

    public BaseBlock getFullBlockRelative(Extent orDefault, int x, int y, int z) {
        return orDefault.getFullBlock(x + getX(), y + getY(), z + getZ());
    }
}
