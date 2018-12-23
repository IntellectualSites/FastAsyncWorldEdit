package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class BlockTranslateExtent extends AbstractDelegateExtent {
    private final int dx, dy, dz;
    private MutableBlockVector mutable = new MutableBlockVector();

    public BlockTranslateExtent(Extent extent, int dx, int dy, int dz) {
        super(extent);
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    @Override
    public boolean setBlock(BlockVector3 location, BlockStateHolder block) throws WorldEditException {
        mutable.mutX((location.getX() + dx));
        mutable.mutY((location.getY() + dy));
        mutable.mutZ((location.getZ() + dz));
        return getExtent().setBlock(mutable, block);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        mutable.mutX(x + dx);
        mutable.mutY(y + dy);
        mutable.mutZ(z + dz);
        return getExtent().setBlock(mutable, block);
    }

    @Override
    public boolean setBiome(BlockVector2 position, BaseBiome biome) {
        return super.setBiome(position.add(dx, dz), biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BaseBiome biome) {
        return super.setBiome(x + dx, y + dy, z + dz, biome);
    }

    @Override
    public BaseBiome getBiome(BlockVector2 position) {
        return super.getBiome(position.add(dx, dz));
    }

    @Override
    public BlockState getBlock(BlockVector3 location) {
        return getLazyBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public BlockState getLazyBlock(BlockVector3 location) {
        return getLazyBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public BlockState getLazyBlock(int x, int y, int z) {
        return super.getLazyBlock(x + dx, y + dy, z + dz);
    }
}
