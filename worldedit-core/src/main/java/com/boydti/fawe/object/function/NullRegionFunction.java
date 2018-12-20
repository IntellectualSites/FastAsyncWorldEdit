package com.boydti.fawe.object.function;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.RegionFunction;

public class NullRegionFunction implements RegionFunction {
    @Override
    public boolean apply(Vector position) throws WorldEditException {
        return false;
    }
}
