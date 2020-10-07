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

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.Vector3Impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a location in a world with has a direction.
 *
 * <p>Like {@code Vectors}, {@code Locations} are immutable and mutator methods
 * will create a new copy.</p>
 *
 * <p>At the moment, but this may change in the future, {@link #hashCode()} and
 * {@link #equals(Object)} are subject to minor differences caused by
 * floating point errors.</p>
 */
public class Location extends Vector3Impl {

    private final Extent extent;
    private final float pitch;
    private final float yaw;

    /**
     * Create a new instance in the given extent at 0, 0, 0 with a
     * direction vector of 0, 0, 0.
     *
     * @param extent the extent
     */
    public Location(Extent extent) {
        this(extent, Vector3.ZERO, 0f, 90f);
    }

    /**
     * Create a new instance in the given extent with the given coordinates
     * with a direction vector of 0, 0, 0.
     *
     * @param extent the extent
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     */
    public Location(Extent extent, double x, double y, double z) {
        this(extent, Vector3.at(x, y, z), 0f, 90f);
    }

    /**
     * Create a new instance in the given extent with the given position
     * vector and a direction vector of 0, 0, 0.
     *
     * @param extent the extent
     * @param position the position vector
     */
    public Location(Extent extent, Vector3 position) {
        this(extent, position, 0f, 90f);
    }

    /**
     * Create a new instance in the given extent with the given coordinates
     * and the given direction vector.
     *
     * @param extent the extent
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @param direction the direction vector
     */
    public Location(Extent extent, double x, double y, double z, Vector3 direction) {
        this(extent, Vector3.at(x, y, z), direction);
    }

    /**
     * Create a new instance in the given extent with the given coordinates
     * and the given direction vector.
     *
     * @param extent the extent
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @param yaw the yaw, in degrees
     * @param pitch the pitch, in degrees
     */
    public Location(Extent extent, double x, double y, double z, float yaw, float pitch) {
        this(extent, Vector3.at(x, y, z), yaw, pitch);
    }

    /**
     * Create a new instance in the given extent with the given position vector
     * and the given direction vector.
     *
     * @param extent the extent
     * @param position the position vector
     * @param direction the direction vector
     */
    public Location(Extent extent, Vector3 position, Vector3 direction) {
        this(extent, position, (float) direction.toYaw(), (float) direction.toPitch());
    }

    /**
     * Create a new instance in the given extent with the given position vector
     * and the given direction vector.
     *
     * @param extent the extent
     * @param position the position vector
     * @param yaw the yaw, in degrees
     * @param pitch the pitch, in degrees
     */
    public Location(Extent extent, Vector3 position, float yaw, float pitch) {
        super(position);
        checkNotNull(extent);
        checkNotNull(position);
        this.extent = extent;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    /**
     * Get the extent.
     *
     * @return the extent
     */
    public Extent getExtent() {
        return extent;
    }

    /**
     * Create a clone of this object with the given extent.
     *
     * @param extent the new extent
     * @return the new instance
     */
    public Location setExtent(Extent extent) {
        return new Location(extent, this, getDirection());
    }

    /**
     * Get the yaw in degrees.
     *
     * @return the yaw in degrees
     */
    public float getYaw() {
        return yaw;
    }

    /**
     * Create a clone of this object with the given yaw.
     *
     * @param yaw the new yaw
     * @return the new instance
     */
    public Location setYaw(float yaw) {
        return new Location(extent, this, yaw, pitch);
    }

    /**
     * Get the pitch in degrees.
     *
     * @return the pitch in degrees
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * Create a clone of this object with the given pitch.
     *
     * @param pitch the new yaw
     * @return the new instance
     */
    public Location setPitch(float pitch) {
        return new Location(extent, this, yaw, pitch);
    }

    /**
     * Create a clone of this object with the given yaw and pitch.
     *
     * @param yaw the new yaw
     * @param pitch the new pitch
     * @return the new instance
     */
    public Location setDirection(float yaw, float pitch) {
        return new Location(extent, this, yaw, pitch);
    }

    /**
     * Get the direction vector.
     *
     * @return the direction vector
     */
    public Vector3 getDirection() {
        if (Float.isNaN(getYaw()) && Float.isNaN(getPitch())) {
            return Vector3.ZERO;
        }
        double yaw = Math.toRadians(this.getYaw());
        double pitch = Math.toRadians(this.getPitch());
        double xz = Math.cos(pitch);
        return Vector3.at(
                -xz * Math.sin(yaw),
                -Math.sin(pitch),
                xz * Math.cos(yaw));
    }

    /**
     * Get the direction as a {@link Direction}.
     *
     * @return The direction
     */
    public Direction getDirectionEnum() {
        return Direction.findClosest(getDirection(), Direction.Flag.ALL);
    }

    /**
     * Create a clone of this object with the given direction.
     *
     * @param direction the new direction
     * @return the new instance
     */
    public Location setDirection(Vector3 direction) {
        return new Location(extent, this, (float) direction.toYaw(), (float) direction.toPitch());
    }

    /**
     * Get a {@link Vector3} form of this location's position.
     *
     * @return a vector
     */
    public Vector3 toVector() {
        return this;
    }

    /**
     * Return a copy of this object with the X component of the new object
     * set to the given value.
     *
     * @param x the new value for the X component
     * @return a new immutable instance
     */
    public Location setX(double x) {
        return new Location(extent, this.withX(x), yaw, pitch);
    }


    /**
     * Return a copy of this object with the Y component of the new object
     * set to the given value.
     *
     * @param y the new value for the Y component
     * @return a new immutable instance
     */
    public Location setY(double y) {
        return new Location(extent, this.withY(y), yaw, pitch);
    }

    /**
     * Return a copy of this object with the Z component of the new object
     * set to the given value.
     *
     * @param z the new value for the Y component
     * @return a new immutable instance
     */
    public Location setZ(double z) {
        return new Location(extent, this.withZ(z), yaw, pitch);
    }

    /**
     * Return a copy of this object with the position set to the given value.
     *
     * @param position The new position
     * @return a new immutable instance
     */
    public Location setPosition(Vector3 position) {
        return new Location(extent, position, yaw, pitch);
    }

    @Override public Location clampY(int min, int max) {
        checkArgument(min <= max, "minimum cannot be greater than maximum");
        if (getY() < min) {
            return new Location(extent, getX(), min, getZ());
        }
        if (getY() > max) {
            return new Location(extent, getX(), max, getZ());
        }
        return this;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Location location = (Location) o;

        if (Double.doubleToLongBits(pitch) != Double.doubleToLongBits(location.pitch)) {
            return false;
        }
        if (Double.doubleToLongBits(yaw) != Double.doubleToLongBits(location.yaw)) {
            return false;
        }
        if (this.getX() != location.getX()) {
            return false;
        }
        if (this.getZ() != location.getZ()) {
            return false;
        }
        if (this.getY() != location.getY()) {
            return false;
        }
        if (!extent.equals(location.extent)) {
            return false;
        }

        return true;
    }

}
