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

package com.sk89q.worldedit;

import java.io.IOException;

/**
 * Extension of {@code Vector} that that compares with other instances
 * using integer components.
 */
public class BlockVector extends Vector {

    public static final BlockVector ZERO = new BlockVector(0, 0, 0);
    public static final BlockVector UNIT_X = new BlockVector(1, 0, 0);
    public static final BlockVector UNIT_Y = new BlockVector(0, 1, 0);
    public static final BlockVector UNIT_Z = new BlockVector(0, 0, 1);
    public static final BlockVector ONE = new BlockVector(1, 1, 1);

    /**
     * Construct an instance as a copy of another instance.
     *
     * @param position the other position
     */
    public BlockVector(Vector position) {
        this(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    /**
     * Construct a new instance.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     */
    public BlockVector(int x, int y, int z) {
        super(x, y, z);
    }

    /**
     * Construct a new instance.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     */
    public BlockVector(float x, float y, float z) {
        this((int) x, (int) y, (int) z);
    }

    /**
     * Construct a new instance.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     */
    public BlockVector(double x, double y, double z) {
        this((int) x, (int) y, (int) z);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Vector)) {
            return false;
        }
        Vector other = (Vector) obj;
        return (int) other.getX() == (int) this.getX() && (int) other.getY() == (int) this.getY()
                && (int) other.getZ() == (int) this.getZ();

    }

    public boolean equals(BlockVector obj) {
        return obj.getBlockX() == this.getBlockX() && obj.getBlockY() == this.getBlockY() && obj.getBlockZ() == this.getBlockZ();
    }

    @Override
    public int hashCode() {
        return ((int) getX() ^ ((int) getZ() << 16)) ^ ((int) getY() << 30);
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        if (!(this instanceof MutableBlockVector)) {
            stream.writeInt(getBlockX());
            stream.writeInt(getBlockY());
            stream.writeInt(getBlockZ());
        }
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        if (this instanceof MutableBlockVector) return;
        this.setComponents(stream.readInt(), stream.readInt(), stream.readInt());
    }

    @Override
    public BlockVector toBlockVector() {
        return this;
    }

    @Override
    public String toString() {
        return "(" + getBlockX() + ", " + getBlockY() + ", " + getBlockZ() + ")";
    }
}
