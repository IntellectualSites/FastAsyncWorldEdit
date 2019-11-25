package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector2;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class OffsetExtent extends ResettableExtent {
    protected final int dx;
    protected final int dy;
    protected final int dz;
    private transient MutableBlockVector2 mutable = new MutableBlockVector2();

    public OffsetExtent(Extent parent, int dx, int dy, int dz) {
        super(parent);
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType biome) {
        return getExtent().setBiome(mutable.setComponents(position.getBlockX() + dx, position.getBlockZ() + dz), biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return getExtent().setBiome(x + dx, y + dy, z + dz, biome);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
        return getExtent().setBlock(location.getBlockX() + dx, location.getBlockY() + dy, location.getBlockZ() + dz, block);
    }

    @Override
    public  <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws WorldEditException {
        return getExtent().setBlock(x + dx, y + dy, z + dz, block);
    }

    @Override
    public ResettableExtent setExtent(Extent extent) {
        mutable = new MutableBlockVector2();
        return super.setExtent(extent);
    }
}
