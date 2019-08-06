package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.PassthroughExtent;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class BlockTranslateExtent extends AbstractDelegateExtent {
    private final int dx, dy, dz;
    private MutableBlockVector3 mutable = new MutableBlockVector3();

    public BlockTranslateExtent(Extent extent, int dx, int dy, int dz) {
        super(extent);
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
        mutable.mutX((location.getX() + dx));
        mutable.mutY((location.getY() + dy));
        mutable.mutZ((location.getZ() + dz));
        return getExtent().setBlock(mutable, block);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws WorldEditException {
        return this.setBlock(BlockVector3.at(x, y, z), block);
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType biome) {
        return super.setBiome(position.add(dx, dz), biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return super.setBiome(x + dx, y + dy, z + dz, biome);
    }

    @Override
    public BiomeType getBiome(BlockVector2 position) {
        return super.getBiome(position.add(dx, dz));
    }

    @Override
    public BlockState getBlock(BlockVector3 location) {
        return getBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ());
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
