package com.boydti.fawe.object.function;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.FlatRegionFunction;
import com.sk89q.worldedit.function.RegionFunction;

public class SurfaceRegionFunction implements FlatRegionFunction {
    private final Extent extent;
    private final RegionFunction function;
    private final int minY;
    private final int maxY;
    private int lastY;
    private MutableBlockVector mutable = new MutableBlockVector();

    public SurfaceRegionFunction(Extent extent, RegionFunction function, int minY, int maxY) {
        this.extent = extent;
        this.minY = minY;
        this.maxY = maxY;
        this.lastY = maxY;
        this.function = function;
    }

    @Override
    public boolean apply(Vector2D position) throws WorldEditException {
        int x = position.getBlockX();
        int z = position.getBlockZ();
        int layer = extent.getNearestSurfaceTerrainBlock(x, z, lastY, minY, maxY, false);
        if (layer != -1) {
            lastY = layer;
            return function.apply(mutable.setComponents(x, layer, z));
        }
        return false;
    }
}
