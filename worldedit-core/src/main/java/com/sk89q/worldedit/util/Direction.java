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

package com.sk89q.worldedit.util;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A collection of cardinal, ordinal, and secondary-ordinal directions.
 */
public enum Direction {

    //FAWE start - left, right
    NORTH(Vector3.at(0, 0, -1), Flag.CARDINAL, 3, 1), // 0
    EAST(Vector3.at(1, 0, 0), Flag.CARDINAL, 0, 2), // 1
    SOUTH(Vector3.at(0, 0, 1), Flag.CARDINAL, 1, 3), // 2
    WEST(Vector3.at(-1, 0, 0), Flag.CARDINAL, 2, 0), // 3

    UP(Vector3.at(0, 1, 0), Flag.UPRIGHT, -1, -1), // 4
    DOWN(Vector3.at(0, -1, 0), Flag.UPRIGHT, -1, -1), // 5

    NORTHEAST(Vector3.at(1, 0, -1), Flag.ORDINAL, 7, 8), // 6
    NORTHWEST(Vector3.at(-1, 0, -1), Flag.ORDINAL, 9, 6), // 7
    SOUTHEAST(Vector3.at(1, 0, 1), Flag.ORDINAL, 6, 9), // 8
    SOUTHWEST(Vector3.at(-1, 0, 1), Flag.ORDINAL, 8, 7), // 9

    WEST_NORTHWEST(Vector3.at(-Math.cos(Math.PI / 8), 0, -Math.sin(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 18, 14), // 11
    WEST_SOUTHWEST(Vector3.at(-Math.cos(Math.PI / 8), 0, Math.sin(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 17, 13), // 12
    NORTH_NORTHWEST(Vector3.at(-Math.sin(Math.PI / 8), 0, -Math.cos(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 12, 15), // 13
    NORTH_NORTHEAST(Vector3.at(Math.sin(Math.PI / 8), 0, -Math.cos(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 11, 16), // 14
    EAST_NORTHEAST(Vector3.at(Math.cos(Math.PI / 8), 0, -Math.sin(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 13, 17), // 15
    EAST_SOUTHEAST(Vector3.at(Math.cos(Math.PI / 8), 0, Math.sin(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 14, 18), // 16
    SOUTH_SOUTHEAST(Vector3.at(Math.sin(Math.PI / 8), 0, Math.cos(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 15, 12), // 17
    SOUTH_SOUTHWEST(Vector3.at(-Math.sin(Math.PI / 8), 0, Math.cos(Math.PI / 8)), Flag.SECONDARY_ORDINAL, 16, 11), // 18

    ASCENDING_NORTH(Vector3.at(0, 1, -1), Flag.ASCENDING_CARDINAL, 3 + 18, 1 + 18), // 19
    ASCENDING_EAST(Vector3.at(1, 1, 0), Flag.ASCENDING_CARDINAL, 0 + 18, 2 + 18), // 20
    ASCENDING_SOUTH(Vector3.at(0, 1, 1), Flag.ASCENDING_CARDINAL, 1 + 18, 3 + 18), // 21
    ASCENDING_WEST(Vector3.at(-1, 1, 0), Flag.ASCENDING_CARDINAL, 2 + 18, 0 + 18), // 22
    ;
    //FAWE end

    private final Vector3 direction;
    private final int flags;
    //FAWE start
    private final int left;
    private final int right;
    //FAWE end
    private final BlockVector3 blockPoint;

    //FAWE start
    private static final HashMap<String, Direction> map = new HashMap<>();

    static {
        for (Direction dir : Direction.values()) {
            map.put(dir.name(), dir);
            map.put(dir.name().toLowerCase(Locale.ROOT), dir);
        }
    }

    Direction(Vector3 vector, int flags, int left, int right) {
        this.blockPoint = BlockVector3.at(Math.signum(vector.x()), Math.signum(vector.y()), Math.signum(vector.z()));
        this.direction = vector.normalize();
        this.flags = flags;
        this.left = left;
        this.right = right;
    }

    public static Direction get(CharSequence sequence) {
        return map.get(sequence);
    }

    /**
     * Get the direction 90 degrees left (anti-clockwise) of this direction if possible, else return this direction
     */
    public Direction getLeft() {
        return left != -1 ? values()[left] : this;
    }

    /**
     * Get the direction 90 degrees right (clockwise) of this direction if possible, else return this direction
     */
    public Direction getRight() {
        return right != -1 ? values()[right] : this;
    }

    public double getX() {
        return direction.x();
    }

    public double getY() {
        return direction.y();
    }

    public double getZ() {
        return direction.z();
    }

    public int getBlockX() {
        return blockPoint.x();
    }

    public int getBlockY() {
        return blockPoint.y();
    }

    public int getBlockZ() {
        return blockPoint.z();
    }
    //FAWE end

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
     * @param flags  the only flags that are permitted (use bitwise math)
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
     * Rotation indexes are used in BlockStates, such as sign posts.
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
            default:
                return Optional.empty();
        }
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
            default:
                return OptionalInt.empty();
        }
    }

    /**
     * Flags to use with {@link #findClosest(Vector3, int)}.
     */
    public static final class Flag {

        public static int CARDINAL = 0x1;
        public static int ORDINAL = 0x2;
        public static int SECONDARY_ORDINAL = 0x4;
        public static int UPRIGHT = 0x8;
        //FAWE start
        public static int ASCENDING_CARDINAL = 0xF;
        //FAWE end

        //FAWE start - ASCENDING_CARDINAL
        public static int ALL = CARDINAL | ORDINAL | SECONDARY_ORDINAL | UPRIGHT | ASCENDING_CARDINAL;
        //FAWE end

        private Flag() {
        }

    }

    //FAWE start - utility methods for block states

    /**
     * Get the directions associated with the given block state, e.g. the connections a fence makes or the direction stairs face
     *
     * @since 2.12.3
     */
    public static EnumSet<Direction> getDirections(BlockState state) {
        EnumSet<Direction> directions = EnumSet.noneOf(Direction.class);
        for (Property<?> property : state.getBlockType().getProperties()) {
            if (property instanceof DirectionalProperty dirProp) {
                directions.add(state.getState(dirProp));
                continue;
            }
            Object value = state.getState(property);
            if (!(value instanceof String str)) {
                if (value instanceof Integer i) {
                    fromRotationIndex(i).ifPresent(directions::add);
                } else if (value instanceof Boolean b && b) {
                    try {
                        directions.add(Direction.valueOf(property.getName().toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                continue;
            }
            switch (str.toLowerCase(Locale.ROOT)) {
                case "upper", "ceiling", "up", "top" -> directions.add(Direction.UP);
                case "lower", "floor", "down", "bottom", "y" -> directions.add(Direction.DOWN);
                case "double", "wall" -> {} // Do nothing
                case "south" -> directions.add(Direction.SOUTH);
                case "x", "east" -> directions.add(Direction.EAST);
                case "z", "north" -> directions.add(Direction.NORTH);
                case "west" -> directions.add(Direction.WEST);
                case "hinge" -> {} // Do nothing for now
                case "shape" -> {} // Do nothing for now
            }
        }
        return directions;
    }
    //FAWE end

}
