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

package com.sk89q.worldedit.util;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;

import javax.annotation.Nullable;
import java.util.HashMap;

/**
 * A collection of cardinal, ordinal, and secondary-ordinal directions.
 */
public enum Direction {

    NORTH(new Vector3(0, 0, -1), Flag.CARDINAL, 3, 1),
    EAST(new Vector3(1, 0, 0), Flag.CARDINAL, 0, 2),
    SOUTH(new Vector3(0, 0, 1), Flag.CARDINAL, 1, 3),
    WEST(new Vector3(-1, 0, 0), Flag.CARDINAL, 2, 0),

    UP(new Vector3(0, 1, 0), Flag.UPRIGHT, -1, -1),
    DOWN(new Vector3(0, -1, 0), Flag.UPRIGHT, -1, -1),

    NORTHEAST(new Vector3(1, 0, -1), Flag.ORDINAL, 7, 8),
    NORTHWEST(new Vector3(-1, 0, -1), Flag.ORDINAL, 9, 6),
    SOUTHEAST(new Vector3(1, 0, 1), Flag.ORDINAL, 6, 9),
    SOUTHWEST(new Vector3(-1, 0, 1), Flag.ORDINAL, 8, 7),

    WEST_NORTHWEST(new Vector3(-Math.cos(Math.PI / 8), 0, -Math.sin(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 9, 6),
    WEST_SOUTHWEST(new Vector3(-Math.cos(Math.PI / 8), 0, Math.sin(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 8, 7),
    NORTH_NORTHWEST(new Vector3(-Math.sin(Math.PI / 8), 0, -Math.cos(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 9, 6),
    NORTH_NORTHEAST(new Vector3(Math.sin(Math.PI / 8), 0, -Math.cos(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 7, 8),
    EAST_NORTHEAST(new Vector3(Math.cos(Math.PI / 8), 0, -Math.sin(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 7, 8),
    EAST_SOUTHEAST(new Vector3(Math.cos(Math.PI / 8), 0, Math.sin(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 6, 9),
    SOUTH_SOUTHEAST(new Vector3(Math.sin(Math.PI / 8), 0, Math.cos(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 6, 9),
    SOUTH_SOUTHWEST(new Vector3(-Math.sin(Math.PI / 8), 0, Math.cos(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 8, 7);

    private final Vector3 direction;
    private final BlockVector3 blockVector;
    private final int flags, left, right;


    private static HashMap<String, Direction> map = new HashMap<>();
    public static final Direction[] values = values();
    public static final Direction[] cardinal = new Direction[]{ NORTH, EAST, SOUTH, WEST };

    static {
        for (Direction dir : Direction.values()) {
            map.put(dir.name(), dir);
            map.put(dir.name().toLowerCase(), dir);
        }
    }

    private Direction(Vector3 vector, int flags, int left, int right) {
        this.direction = vector.normalize();
        this.blockVector = new BlockVector3(Math.signum(vector.getX()), Math.signum(vector.getY()), Math.signum(vector.getZ()));
        this.flags = flags;
        this.left = left;
        this.right = right;
    }

    public static Direction get(CharSequence sequence) {
        return map.get(sequence);
    }

    public Direction getLeft() {
        return left != -1 ? values[left] : null;
    }

    public Direction getRight() {
        return right != -1 ? values[right] : null;
    }

    public double getX() {
        return direction.getX();
    }

    public double getY() {
        return direction.getY();
    }

    public double getZ() {
        return direction.getZ();
    }

    public int getBlockX() {
        return blockVector.getBlockX();
    }

    public int getBlockY() {
        return blockVector.getBlockY();
    }

    public int getBlockZ() {
        return blockVector.getBlockZ();
    }

    /**
     * Return true if the direction is of a cardinal direction (north, west
     * east, and south).
     *
     * <p>This evaluates as false for directions that have a non-zero
     * Y-component.</p>
     *
     * @return true if cardinal
     */
    public boolean isCardinal() {
        return (flags & Flag.CARDINAL) > 0;
    }

    /**
     * Return true if the direction is of an ordinal direction (northwest,
     * southwest, southeast, northeaast).
     *
     * @return true if ordinal
     */
    public boolean isOrdinal() {
        return (flags & Flag.ORDINAL) > 0;
    }

    /**
     * Return true if the direction is of a secondary ordinal direction
     * (north-northwest, north-northeast, south-southwest, etc.).
     *
     * @return true if secondary ordinal
     */
    public boolean isSecondaryOrdinal() {
        return (flags & Flag.SECONDARY_ORDINAL) > 0;
    }

    /**
     * Return whether Y component is non-zero.
     *
     * @return true if the Y component is non-zero
     */
    public boolean isUpright() {
        return (flags & Flag.UPRIGHT) > 0;
    }

    /**
     * Get the vector.
     *
     * @return the vector
     */
    public Vector3 toVector() {
        return direction;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    /**
     * Get the vector.
     *
     * @return the vector
     */
    public BlockVector3 toBlockVector() {
        return direction.toBlockPoint();
    }

    /**
     * Find the closest direction to the given direction vector.
     *
     * @param vector the vector
     * @param flags the only flags that are permitted (use bitwise math)
     * @return the closest direction, or null if no direction can be returned
     */
    @Nullable
    public static Direction findClosest(Vector3 vector, int flags) {
        if ((flags & Flag.UPRIGHT) == 0) {
            vector = vector.withY(0);
        }
        vector = vector.normalize();

        Direction closest = null;
        double closestDot = -2;
        for (Direction direction : values()) {
            if ((~flags & direction.flags) > 0) {
                continue;
            }

            double dot = direction.toVector().dot(vector);
            if (dot >= closestDot) {
                closest = direction;
                closestDot = dot;
            }
        }

        return closest;
    }

    /**
     * Flags to use with {@link #findClosest(Vector3, int)}.
     */
    public static final class Flag {
        public static int CARDINAL = 0x1;
        public static int ORDINAL = 0x2;
        public static int SECONDARY_ORDINAL = 0x4;
        public static int UPRIGHT = 0x8;

        public static int ALL = CARDINAL | ORDINAL | SECONDARY_ORDINAL | UPRIGHT;

        private Flag() {
        }
    }

}

