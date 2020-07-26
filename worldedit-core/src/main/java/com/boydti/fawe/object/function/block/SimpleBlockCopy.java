package com.boydti.fawe.object.function.block;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.math.BlockVector3;

public class SimpleBlockCopy implements RegionFunction {

    protected final Extent source;
    protected final Extent destination;

    public SimpleBlockCopy(Extent source, Extent destination) {
        this.source = source;
        this.destination = destination;
    }

    @Override
    public boolean apply(BlockVector3 position) throws WorldEditException {
        return destination.setBlock(position, source.getFullBlock(position));
    }
}
