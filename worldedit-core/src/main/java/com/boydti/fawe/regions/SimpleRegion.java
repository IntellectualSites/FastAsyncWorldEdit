package com.boydti.fawe.regions;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.AbstractRegion;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.world.World;

public abstract class SimpleRegion extends AbstractRegion {
    private final BlockVector3 max;
    private final BlockVector3 min;

    public SimpleRegion(World world, BlockVector3 min, BlockVector3 max) {
        super(world);
        this.min = min;
        this.max = max;
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return min;
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return max;
    }

    @Override
    public void expand(BlockVector3... changes) throws RegionOperationException {
        throw new UnsupportedOperationException("Region is immutable");
    }

    @Override
    public void contract(BlockVector3... changes) throws RegionOperationException {
        throw new UnsupportedOperationException("Region is immutable");
    }

    @Override
    public boolean contains(BlockVector3 p) {
        return contains(p.getBlockX(), p.getBlockY(), p.getBlockZ());
    }

    @Override
    public abstract boolean contains(int x, int y, int z);

    @Override
    public abstract boolean contains(int x, int z);
}
