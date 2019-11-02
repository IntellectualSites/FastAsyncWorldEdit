package com.boydti.fawe.beta.implementation.filter.block;

import com.boydti.fawe.beta.Filter;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import javax.annotation.Nullable;

public class ArrayFilterBlock extends SimpleFilterBlock {

    private final char[] blocks;
    private final byte[] heights;
    private final int yOffset;
    private final int width, length;
    private int x, z, index;
    private char ordinal;

    public ArrayFilterBlock(Extent extent, char[] blocks, byte[] heights, int width, int length,
        int yOffset) {
        super(extent);
        this.blocks = blocks;
        this.width = width;
        this.length = length;
        this.heights = heights;
        this.yOffset = yOffset;
    }

    public void filter2D(Filter filter) {
        for (z = 0; z < length; z++) {
            for (x = 0; x < width; x++, index++) {
                ordinal = blocks[ordinal];
                filter.applyBlock(this);
            }
        }
    }

    @Override
    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public void setOrdinal(int ordinal) {
        blocks[index] = (char) ordinal;
    }

    @Override
    public BlockState getBlock() {
        return BlockTypesCache.states[ordinal];
    }

    @Override
    public void setBlock(BlockState state) {
        blocks[index] = state.getOrdinalChar();
    }

    @Override
    public BaseBlock getFullBlock() {
        return getBlock().toBaseBlock();
    }

    @Override
    public void setFullBlock(BaseBlock block) {
        blocks[index] = block.getOrdinalChar();
    }

    @Override
    public CompoundTag getNbtData() {
        return null;
    }

    @Override
    public void setNbtData(@Nullable CompoundTag nbtData) {
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return (heights[index] & 0xFF) + yOffset;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block)
        throws WorldEditException {
        return getExtent().setBlock(x,y, z, block);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return getExtent().setBiome(x,y, z,biome);
    }
}
