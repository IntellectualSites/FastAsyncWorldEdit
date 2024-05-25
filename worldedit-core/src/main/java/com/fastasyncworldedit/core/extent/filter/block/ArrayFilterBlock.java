package com.fastasyncworldedit.core.extent.filter.block;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import javax.annotation.Nullable;

/**
 * @deprecated Unused internally
 */
@Deprecated(forRemoval = true, since = "2.3.0")
public class ArrayFilterBlock extends AbstractExtentFilterBlock {

    private final char[] blocks;
    private final byte[] heights;
    private final int yOffset;
    private final int width;
    private final int length;
    private int x;
    private int z;
    private int index;

    public ArrayFilterBlock(
            Extent extent, char[] blocks, byte[] heights, int width, int length,
            int yOffset
    ) {
        super(extent);
        this.blocks = blocks;
        this.width = width;
        this.length = length;
        this.heights = heights;
        this.yOffset = yOffset;
    }

    public void init(int x, int z, int index) {
        this.x = x;
        this.z = z;
        this.index = index;
    }


    @Override
    public int getOrdinal() {
        return blocks[index];
    }

    @Override
    public void setOrdinal(int ordinal) {
        blocks[index] = (char) ordinal;
    }

    @Override
    public BlockState getBlock() {
        return BlockTypesCache.states[getOrdinal()];
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
    public int x() {
        return x;
    }

    @Override
    public int y() {
        return (heights[index] & 0xFF) + yOffset;
    }

    @Override
    public int z() {
        return z;
    }

    @Override
    public void setBlock(BlockState state) {
        blocks[index] = state.getOrdinalChar();
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block)
            throws WorldEditException {
        return getExtent().setBlock(x, y, z, block);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return getExtent().setBiome(x, y, z, biome);
    }

    @Override
    public void setBiome(final BiomeType biome) {
        getExtent().setBiome(x(), y(), z(), biome);
    }

    @Override
    public BiomeType getBiome() {
        return getExtent().getBiomeType(x(), y(), z());
    }

}
