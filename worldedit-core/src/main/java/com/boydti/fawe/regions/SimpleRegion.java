package com.boydti.fawe.regions;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.AbstractRegion;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.world.World;

public abstract class SimpleRegion extends AbstractRegion {
    private final Vector max;
    private final Vector min;

    public SimpleRegion(World world, Vector min, Vector max) {
        super(world);
        this.min = min;
        this.max = max;
    }

    @Override
    public Vector getMinimumPoint() {
        return min;
    }

    @Override
    public Vector getMaximumPoint() {
        return max;
    }

    @Override
    public void expand(Vector... changes) throws RegionOperationException {
        throw new UnsupportedOperationException("Region is immutable");
    }

    @Override
    public void contract(Vector... changes) throws RegionOperationException {
        throw new UnsupportedOperationException("Region is immutable");
    }

    @Override
    public boolean contains(Vector p) {
        return contains(p.getBlockX(), p.getBlockY(), p.getBlockZ());
    }

    @Override
    public abstract boolean contains(int x, int y, int z);

    @Override
    public abstract boolean contains(int x, int z);
}
