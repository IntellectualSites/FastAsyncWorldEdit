package com.fastasyncworldedit.core.function.block;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.math.BlockVector3;

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
        int x = position.x();
        int y = position.y();
        int z = position.z();
        if (x != mutableVector.x() || z != mutableVector.z() || y != mutableVector
                .y()) {
            mutableVector.setComponents(x, y, z);
            return destination.setBiome(mutableVector, source.getBiome(mutableVector));
        }
        return false;
    }

}
