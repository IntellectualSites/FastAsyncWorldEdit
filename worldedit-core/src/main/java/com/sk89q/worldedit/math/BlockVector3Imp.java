/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.math;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ComparisonChain;
import com.sk89q.worldedit.math.transform.AffineTransform;

import java.util.Comparator;

/**
 * An immutable 3-dimensional vector.
 */
public class BlockVector3Imp extends BlockVector3 {

    public static final BlockVector3Imp ZERO   = new BlockVector3Imp(0, 0, 0);
    public static final BlockVector3Imp UNIT_X = new BlockVector3Imp(1, 0, 0);
    public static final BlockVector3Imp UNIT_Y = new BlockVector3Imp(0, 1, 0);
    public static final BlockVector3Imp UNIT_Z = new BlockVector3Imp(0, 0, 1);
    public static final BlockVector3Imp ONE    = new BlockVector3Imp(1, 1, 1);

    public static BlockVector3Imp at(double x, double y, double z) {
        return at((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public static BlockVector3Imp at(int x, int y, int z) {
        return new BlockVector3Imp(x, y, z);
    }

    private final int x, y, z;

    /**
     * Construct an instance.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     */
    protected BlockVector3Imp(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public final int getX() {
        return x;
    }

    @Override
    public final int getY() {
        return y;
    }

    @Override
    public final int getZ() {
        return z;
    }

    @Override
    public int hashCode() {
        return (getX() ^ (getZ() << 12)) ^ (getY() << 24);
    }

    @Override
    public String toString() {
        return "(" + getX() + ", " + getY() + ", " + getZ() + ")";
    }
}
