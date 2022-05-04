package com.fastasyncworldedit.core.util.collection;

import com.fastasyncworldedit.core.math.BlockVectorSet;
import com.fastasyncworldedit.core.math.LocalBlockVectorSet;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import java.util.Set;

public interface BlockVector3Set extends Set<BlockVector3> {

    static BlockVector3Set getAppropriateVectorSet(Region region) {
        BlockVector3 max = region.getMaximumPoint();
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 size = region.getDimensions();
        if (size.getBlockX() > 2048 || size.getBlockZ() > 2048 || size.getBlockY() > 512) {
            return new BlockVectorSet();
        } else {
            // Set default offset as many operations utilising a region are likely to start in a corner, this initialising the
            // LocalBlockVectorSet poorly
            // This needs to be ceiling as LocalBlockVector extends 1 block further "negative"
            int offsetX = (int) Math.ceil((min.getX() + max.getX()) / 2d);
            int offsetZ = (int) Math.ceil((min.getZ() + max.getZ()) / 2d);
            int offsetY;
            if (region.getMinimumY() < -128 || region.getMaximumY() > 320) {
                offsetY = (min.getY() + max.getY()) / 2;
            } else {
                offsetY = 128;
            }
            return new LocalBlockVectorSet(offsetX, offsetY, offsetZ);
        }
    }
    boolean add(int x, int y, int z);

    boolean contains(int x, int y, int z);

}
