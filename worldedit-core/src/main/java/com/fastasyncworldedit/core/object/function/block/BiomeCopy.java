package com.fastasyncworldedit.core.object.function.block;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;

public class BiomeCopy implements RegionFunction {

    protected final Extent source;
    protected final Extent destination;
    private final MutableBlockVector3 mutableVector;

    public BiomeCopy(Extent source, Extent destination) {
        this.source = source;
        this.destination = destination;
        this.mutableVector = new MutableBlockVector3();
        this.mutableVector.setComponents(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    @Override
    public boolean apply(BlockVector3 position) throws WorldEditException {
        int x = position.getBlockX();
        int y = position.getBlockY();
        int z = position.getBlockZ();
        if (x != mutableVector.getBlockX() || z != mutableVector.getBlockZ() || y != mutableVector
            .getBlockY()) {
            mutableVector.setComponents(x, y, z);
            return destination.setBiome(mutableVector, source.getBiome(mutableVector));
        }
        return false;
    }
}
