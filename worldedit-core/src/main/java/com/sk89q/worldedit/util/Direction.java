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
import java.util.*;

/**
 * A collection of cardinal, ordinal, and secondary-ordinal directions.
 */
public enum Direction {

    NORTH(Vector3.at(0, 0, -1), Flag.CARDINAL, 3, 1),
    EAST(Vector3.at(1, 0, 0), Flag.CARDINAL, 0, 2),
    SOUTH(Vector3.at(0, 0, 1), Flag.CARDINAL, 1, 3),
    WEST(Vector3.at(-1, 0, 0), Flag.CARDINAL, 2, 0),

    UP(Vector3.at(0, 1, 0), Flag.UPRIGHT, -1, -1),
    DOWN(Vector3.at(0, -1, 0), Flag.UPRIGHT, -1, -1),

    NORTHEAST(Vector3.at(1, 0, -1), Flag.ORDINAL, 7, 8),
    NORTHWEST(Vector3.at(-1, 0, -1), Flag.ORDINAL, 9, 6),
    SOUTHEAST(Vector3.at(1, 0, 1), Flag.ORDINAL, 6, 9),
    SOUTHWEST(Vector3.at(-1, 0, 1), Flag.ORDINAL, 8, 7),

    WEST_NORTHWEST(Vector3.at(-Math.cos(Math.PI / 8), 0, -Math.sin(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 9, 6),
    WEST_SOUTHWEST(Vector3.at(-Math.cos(Math.PI / 8), 0, Math.sin(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 8, 7),
    NORTH_NORTHWEST(Vector3.at(-Math.sin(Math.PI / 8), 0, -Math.cos(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 9, 6),
    NORTH_NORTHEAST(Vector3.at(Math.sin(Math.PI / 8), 0, -Math.cos(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 7, 8),
    EAST_NORTHEAST(Vector3.at(Math.cos(Math.PI / 8), 0, -Math.sin(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 7, 8),
    EAST_SOUTHEAST(Vector3.at(Math.cos(Math.PI / 8), 0, Math.sin(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 6, 9),
    SOUTH_SOUTHEAST(Vector3.at(Math.sin(Math.PI / 8), 0, Math.cos(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 6, 9),
    SOUTH_SOUTHWEST(Vector3.at(-Math.sin(Math.PI / 8), 0, Math.cos(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 8, 7),

    ASCENDING_NORTH(Vector3.at(0, 1, -1), Flag.ASCENDING_CARDINAL, 3 + 18, 1 + 18),
    ASCENDING_EAST(Vector3.at(1, 1, 0), Flag.ASCENDING_CARDINAL, 0 + 18, 2 + 18),
    ASCENDING_SOUTH(Vector3.at(0, 1, 1), Flag.ASCENDING_CARDINAL, 1 + 18, 3 + 18),
    ASCENDING_WEST(Vector3.at(-1, 1, 0), Flag.ASCENDING_CARDINAL, 2 + 18, 0 + 18),
    ;

    private final Vector3 direction;
    private final int flags;
    private final int left;
    private final int right;
    private final BlockVector3 blockPoint;

    private static HashMap<String, Direction> map = new HashMap<>();
    
    static {
        for (Direction dir : Direction.values()) {
            map.put(dir.name(), dir);
            map.put(dir.name().toLowerCase(), dir);
        }
    }

    Direction(Vector3 vector, int flags, int left, int right) {
        this.blockPoint = vector.toBlockPoint();
        this.direction = vector.normalize();
        this.flags = flags;
        this.left = left;
        this.right = right;
    }
    
    public static Direction get(CharSequence sequence) {
        return map.get(sequence.toString());
    }
    
    public Direction getLeft() {
        return left != -1 ? values()[left] : null;
    }

    public Direction getRight() {
        return right != -1 ? values()[right] : null;
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

    /**
     * Get the vector.
     *
     * @return the vector
     */
    public BlockVector3 toBlockVector() {
        return blockPoint;
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
     * Gets all directions with the given flags.
     *
     * @param flags The flags
     * @return The directions that fit the flags
     */
    public static List<Direction> valuesOf(int flags) {
        List<Direction> directions = new ArrayList<>();
        for (Direction direction : values()) {
            if ((~flags & direction.flags) == 0) {
                directions.add(direction);
            }
        }

        return directions;
    }

    /**
     * Converts a rotation index into a Direction.
     *
     * <p>
     *     Rotation indexes are used in BlockStates, such as sign posts.
     * </p>
     *
     * @param rotation The rotation index
     * @return The direction, if applicable
     */
    public static Optional<Direction> fromRotationIndex(int rotation) {
        switch (rotation) {
            case 0:
                return Optional.of(SOUTH);
            case 1:
                return Optional.of(SOUTH_SOUTHWEST);
            case 2:
                return Optional.of(SOUTHWEST);
            case 3:
                return Optional.of(WEST_SOUTHWEST);
            case 4:
                return Optional.of(WEST);
            case 5:
                return Optional.of(WEST_NORTHWEST);
            case 6:
                return Optional.of(NORTHWEST);
            case 7:
                return Optional.of(NORTH_NORTHWEST);
            case 8:
                return Optional.of(NORTH);
            case 9:
                return Optional.of(NORTH_NORTHEAST);
            case 10:
                return Optional.of(NORTHEAST);
            case 11:
                return Optional.of(EAST_NORTHEAST);
            case 12:
                return Optional.of(EAST);
            case 13:
                return Optional.of(EAST_SOUTHEAST);
            case 14:
                return Optional.of(SOUTHEAST);
            case 15:
                return Optional.of(SOUTH_SOUTHEAST);
        }

        return Optional.empty();
    }

    public OptionalInt toRotationIndex() {
        switch (this) {
            case SOUTH:
                return OptionalInt.of(0);
            case SOUTH_SOUTHWEST:
                return OptionalInt.of(1);
            case SOUTHWEST:
                return OptionalInt.of(2);
            case WEST_SOUTHWEST:
                return OptionalInt.of(3);
            case WEST:
                return OptionalInt.of(4);
            case WEST_NORTHWEST:
                return OptionalInt.of(5);
            case NORTHWEST:
                return OptionalInt.of(6);
            case NORTH_NORTHWEST:
                return OptionalInt.of(7);
            case NORTH:
                return OptionalInt.of(8);
            case NORTH_NORTHEAST:
                return OptionalInt.of(9);
            case NORTHEAST:
                return OptionalInt.of(10);
            case EAST_NORTHEAST:
                return OptionalInt.of(11);
            case EAST:
                return OptionalInt.of(12);
            case EAST_SOUTHEAST:
                return OptionalInt.of(13);
            case SOUTHEAST:
                return OptionalInt.of(14);
            case SOUTH_SOUTHEAST:
                return OptionalInt.of(15);
        }
        return OptionalInt.empty();
    }

    /**
     * Flags to use with {@link #findClosest(Vector3, int)}.
     */
    public static final class Flag {
        public static int CARDINAL = 0x1;
        public static int ORDINAL = 0x2;
        public static int SECONDARY_ORDINAL = 0x4;
        public static int UPRIGHT = 0x8;
        public static int ASCENDING_CARDINAL = 0xF;

        public static int ALL = CARDINAL | ORDINAL | SECONDARY_ORDINAL | UPRIGHT | ASCENDING_CARDINAL;

        private Flag() {
        }
    }

}

