package com.boydti.fawe.beta;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

public class NorthVector extends BlockVector3 {
    private final BlockVector3 parent;

    public NorthVector(BlockVector3 parent) {
        this.parent = parent;
    }

    @Override
    public BlockVector3 south(BlockVector3 orDefault) {
        return parent;
    }

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

    public boolean setOrdinal(Extent orDefault, int ordinal) {
        return orDefault.setBlock(this, BlockState.getFromOrdinal(ordinal));
    }

    public boolean setBlock(Extent orDefault, BlockState state) {
        return orDefault.setBlock(this, state);
    }

    public boolean setFullBlock(Extent orDefault, BaseBlock block) {
        return  orDefault.setBlock(this, block);
    }

    public boolean setBiome(Extent orDefault, BiomeType biome) {
        return orDefault.setBiome(getX(), getY(), getZ(), biome);
    }

    public int getOrdinal(Extent orDefault) {
        return getBlock(orDefault).getOrdinal();
    }

    public char getOrdinalChar(Extent orDefault) {
        return (char) getOrdinal(orDefault);
    }

    public BlockState getBlock(Extent orDefault) {
        return orDefault.getBlock(this);
    }

    public BaseBlock getFullBlock(Extent orDefault) {
        return orDefault.getFullBlock(this);
    }

    public CompoundTag getNbtData(Extent orDefault) {
        return orDefault.getFullBlock(getX(), getY(), getZ()).getNbtData();
    }

    public BlockState getOrdinalBelow(Extent orDefault) {
        return getStateRelative(orDefault, 0, -1, 0);
    }

    public BlockState getStateAbove(Extent orDefault) {
        return getStateRelative(orDefault, 0, 1, 0);
    }

    public BlockState getStateRelativeY(Extent orDefault, final int y) {
        return getStateRelative(orDefault, 0, y, 0);
    }

    public BlockState getStateRelative(Extent orDefault, final int x, final int y, final int z) {
        return getFullBlockRelative(orDefault, x, y, z).toBlockState();
    }

    public BaseBlock getFullBlockRelative(Extent orDefault, int x, int y, int z) {
        return orDefault.getFullBlock(x + getX(), y + getY(), z + getZ());
    }
}
