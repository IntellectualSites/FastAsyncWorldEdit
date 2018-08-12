package com.sk89q.worldedit.function.visitor;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.RegionFunction;

public class IntersectRegionFunction implements RegionFunction {
    private final RegionFunction[] functions;

    public IntersectRegionFunction(RegionFunction... functions) {
        this.functions = functions;
    }


    @Override
    public boolean apply(Vector position) throws WorldEditException {
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
