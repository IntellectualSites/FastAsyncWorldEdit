package com.boydti.fawe.object.function.block;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.RegionFunction;

public class SimpleBlockCopy implements RegionFunction {

    protected final Extent source;
    protected final Extent destination;

    public SimpleBlockCopy(Extent source, Extent destination) {
        this.source = source;
        this.destination = destination;
    }

    @Override
    public boolean apply(Vector position) throws WorldEditException {
        return destination.setBlock(position, source.getBlock(position));
    }
}
