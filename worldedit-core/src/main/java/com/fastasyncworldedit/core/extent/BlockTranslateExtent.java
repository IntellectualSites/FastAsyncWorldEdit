package com.fastasyncworldedit.core.extent;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public final class BlockTranslateExtent extends AbstractDelegateExtent {

    private final int dx;
    private final int dy;
    private final int dz;

    public BlockTranslateExtent(Extent extent, int dx, int dy, int dz) {
        super(extent);
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
        return getExtent().setBlock(location.x() + dx, location.y() + dy, location.z() + dz, block);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws WorldEditException {
        return getExtent().setBlock(x + dx, y + dy, z + dz, block);
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        return super.setBiome(position.add(dx, dy, dz), biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return super.setBiome(x + dx, y + dy, z + dz, biome);
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        return super.getBiome(position.add(dx, dy, dz));
    }

    @Override
    public BlockState getBlock(BlockVector3 location) {
        return getBlock(location.x(), location.y(), location.z());
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return super.getBlock(x + dx, y + dy, z + dz);
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 pos) {
        return super.getFullBlock(pos.add(dx, dy, dz));
    }

}
