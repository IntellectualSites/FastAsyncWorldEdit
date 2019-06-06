package com.boydti.fawe.object;

import com.boydti.fawe.FaweAPI;
import com.sk89q.worldedit.math.*;
import com.sk89q.worldedit.world.World;
import jdk.nashorn.internal.ir.Block;

/**
 * @deprecated This is likely to be removed in favor of {@link com.sk89q.worldedit.util.Location}.
 */
@Deprecated
public class FaweLocation {

    public final BlockVector3 vector;
    @Deprecated
    public final int x;
    @Deprecated
    public final int z;
    @Deprecated
    public final int y;
    public final String world;

    public FaweLocation(final String world, final int x, final int y, final int z) {
        this.world = world;
        this.vector = BlockVector3.at(x,y,z);
        this.x = vector.getX();
        this.y = vector.getY();
        this.z = vector.getZ();
    }

    /**
     * Get the X coordinate.
     *
     * @return the x coordinate
     */
    public int getX() {
        return vector.getX();
    }

    /**
     * Get the Y coordinate.
     *
     * @return the y coordinate
     */
    public int getY() {
        return vector.getY();
    }

    /**
     * Get the Z coordinate.
     *
     * @return the z coordinate
     */
    public int getZ() {
        return vector.getZ();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final FaweLocation other = (FaweLocation) obj;
        return ((this.x == other.x) && (this.y == other.y) && (this.z == other.z) && (this.world.equals(other.world)));
    }

    public World getWorld() {
        return FaweAPI.getWorld(world);
    }

    @Override
    public String toString() {
        return world + "," + x + "," + y + "," + z;
    }

    @Override
    public int hashCode() {
        return this.x << (8 + this.z) << (4 + this.y);
    }
}
