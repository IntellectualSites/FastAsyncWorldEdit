package com.fastasyncworldedit.core.util.collection;

import com.fastasyncworldedit.core.math.BlockVectorSet;
import com.fastasyncworldedit.core.math.LocalBlockVectorSet;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import java.util.Set;

public interface BlockVector3Set extends Set<BlockVector3> {

    /**
     * Get the appropriate {@link BlockVector3Set} implementation for the given region. Either {@link LocalBlockVectorSet} or
     * {@link BlockVectorSet}. Sets the offset if using {@link LocalBlockVectorSet}.
     *
     * @param region Region to get for
     * @return Appropriate {@link BlockVector3Set} implementation
     */
    static BlockVector3Set getAppropriateVectorSet(Region region) {
        BlockVector3 max = region.getMaximumPoint();
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3Set set = getAppropriateVectorSet(region.getDimensions());
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
        set.setOffset(offsetX, offsetY, offsetZ);
        return set;
    }

    /**
     * Get the appropriate {@link BlockVector3Set} implementation for the given dimensions. Either {@link LocalBlockVectorSet} or
     * {@link BlockVectorSet}. The offset should be manually set.
     *
     * @param size Dimensions to get for
     * @return Appropriate {@link BlockVector3Set} implementation
     */
    static BlockVector3Set getAppropriateVectorSet(BlockVector3 size) {
        if (size.getBlockX() > 2048 || size.getBlockZ() > 2048 || size.getBlockY() > 512) {
            return new BlockVectorSet();
        } else {
            return new LocalBlockVectorSet();
        }
    }
    boolean add(int x, int y, int z);

    boolean contains(int x, int y, int z);

    /**
     * Set the offset applied to values when storing and reading to keep the values within -1024 to 1023. Uses default y offset
     * of 128 to allow -64 -> 320 world height use.
     *
     * @param x x offset
     * @param z z offset
     * @since TODO
     */
    void setOffset(int x, int z);

    /**
     * Set the offset applied to values when storing and reading to keep the x and z values within -1024 to 1023. Y values
     * require keeping withing -256 and 255.
     *
     * @param x x offset
     * @param y y offset
     * @param z z offset
     * @since TODO
     */
    void setOffset(int x, int y, int z);

    /**
     * If a radius is contained by the set
     *
     * @param x      x radius center
     * @param y      y radius center
     * @param z      z radius center
     * @return if radius is contained by the set
     * @since TODO
     */
    boolean containsRadius(int x, int y, int z, int radius);

}
