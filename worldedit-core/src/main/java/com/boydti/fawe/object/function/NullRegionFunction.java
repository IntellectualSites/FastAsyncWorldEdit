package com.boydti.fawe.object.function;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.math.BlockVector3;

public class NullRegionFunction implements RegionFunction {
    @Override
    public boolean apply(BlockVector3 position) throws WorldEditException {
        return false;
    }
}
