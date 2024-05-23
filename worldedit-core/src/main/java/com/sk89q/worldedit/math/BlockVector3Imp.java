/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.math;

/**
 * An immutable 3-dimensional vector.
 */
public final class BlockVector3Imp extends BlockVector3 {

    public static final BlockVector3Imp ZERO = new BlockVector3Imp(0, 0, 0);
    public static final BlockVector3Imp UNIT_X = new BlockVector3Imp(1, 0, 0);
    public static final BlockVector3Imp UNIT_Y = new BlockVector3Imp(0, 1, 0);
    public static final BlockVector3Imp UNIT_Z = new BlockVector3Imp(0, 0, 1);
    public static final BlockVector3Imp ONE = new BlockVector3Imp(1, 1, 1);

    public static BlockVector3Imp at(double x, double y, double z) {
        return at((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public static BlockVector3Imp at(int x, int y, int z) {
        return new BlockVector3Imp(x, y, z);
    }

    private final int x;
    private final int y;
    private final int z;

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
    public int x() {
        return x;
    }

    @Override
    public int y() {
        return y;
    }

    @Override
    public int z() {
        return z;
    }

    @Override
    public int hashCode() {
        return (x() ^ (z() << 12)) ^ (y() << 24);
    }

    @Override
    public BlockVector3 toImmutable() {
        return this;
    }

    @Override
    public String toString() {
        return "(" + x() + ", " + y() + ", " + z() + ")";
    }

}
