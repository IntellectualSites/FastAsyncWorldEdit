package com.fastasyncworldedit.core.function.mask;

import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

public class WallMakeMask implements Mask {

    private final Region region;

    public WallMakeMask(Region region) {
        this.region = region.clone();
    }

    @Override
    public boolean test(BlockVector3 position) {
        int x = position.x();
        int z = position.z();
        return !region.contains(x, z + 1) || !region.contains(x, z - 1) || !region.contains(x + 1, z) || !region.contains(
                x - 1,
                z
        );
    }

    @Override
    public Mask copy() {
        return new WallMakeMask(region);
    }

}
