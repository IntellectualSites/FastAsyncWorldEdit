package com.fastasyncworldedit.core.function;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.FlatRegionFunction;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.math.BlockVector2;

public class SurfaceRegionFunction implements FlatRegionFunction {

    private final Extent extent;
    private final RegionFunction function;
    private final int minY;
    private final int maxY;
    private int lastY;
    private final MutableBlockVector3 mutable = new MutableBlockVector3();

    public SurfaceRegionFunction(Extent extent, RegionFunction function, int minY, int maxY) {
        this.extent = extent;
        this.minY = minY;
        this.maxY = maxY;
        this.lastY = maxY;
        this.function = function;
    }

    @Override
    public boolean apply(BlockVector2 position) throws WorldEditException {
        int x = position.x();
        int z = position.z();
        int layer = extent.getNearestSurfaceTerrainBlock(x, z, lastY, minY, maxY, false);
        if (layer != -1) {
            lastY = layer;
            return function.apply(mutable.setComponents(x, layer, z));
        }
        return false;
    }

}
