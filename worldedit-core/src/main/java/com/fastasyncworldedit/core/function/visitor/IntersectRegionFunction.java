package com.fastasyncworldedit.core.function.visitor;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.math.BlockVector3;

public record IntersectRegionFunction(RegionFunction... functions) implements RegionFunction {

    @Override
    public boolean apply(BlockVector3 position) throws WorldEditException {
        boolean ret = false;
        for (RegionFunction function : functions) {
            if (!function.apply(position)) {
                return ret;
            } else {
                ret = true;
            }
        }
        return ret;
    }

}
