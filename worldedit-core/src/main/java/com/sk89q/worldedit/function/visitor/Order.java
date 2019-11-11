package com.sk89q.worldedit.function.visitor;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

import java.util.Iterator;

public enum Order {
    YZX() {
        @Override
        public Iterator<BlockVector3> create(Region region) {
            if (!(region instanceof CuboidRegion)) {
                region = new CuboidRegion(region.getMinimumPoint(), region.getMaximumPoint());
            }
            return ((CuboidRegion) region).iterator_old();
        }
    }

    ;

    public abstract Iterator<BlockVector3> create(Region region);
}
