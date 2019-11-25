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

import com.boydti.fawe.util.MathMan;
import com.google.common.collect.ComparisonChain;
import com.sk89q.worldedit.math.transform.AffineTransform;

import java.util.Comparator;

/**
 * An immutable 3-dimensional vector.
 */
public abstract class Vector3 {

    public static final Vector3 ZERO = Vector3.at(0, 0, 0);
    public static final Vector3 UNIT_X = Vector3.at(1, 0, 0);
    public static final Vector3 UNIT_Y = Vector3.at(0, 1, 0);
    public static final Vector3 UNIT_Z = Vector3.at(0, 0, 1);
    public static final Vector3 ONE = Vector3.at(1, 1, 1);

    public static Vector3 at(double x, double y, double z) {
        /* Unnecessary
        // switch for efficiency on typical cases
        // in MC y is rarely 0/1 on selections
        int yTrunc = (int) y;
        switch (yTrunc) {
            case 0:
                if (x == 0 && y == 0 && z == 0) {
                    return ZERO;
                }
                break;
            case 1:
                if (x == 1 && y == 1 && z == 1) {
                    return ONE;
                }
                break;
        }
        */
        return new Vector3Impl(x, y, z);
    }

    // thread-safe initialization idiom
    private static final class YzxOrderComparator {
        private static final Comparator<Vector3> YZX_ORDER = (a, b) -> {
            return ComparisonChain.start()
                    .compare(a.getY(), b.getY())
                    .compare(a.getZ(), b.getZ())
                    .compare(a.getX(), b.getX())
                    .result();
        };
    }

    /**
     * Returns a comparator that sorts vectors first by Y, then Z, then X.
     *
     * <p>
     * Useful for sorting by chunk block storage order.
     */
    public static Comparator<Vector3> sortByCoordsYzx() {
        return YzxOrderComparator.YZX_ORDER;
    }

    public int getBlockX() {
        return MathMan.roundInt(getX());
    }

    public int getBlockY() {
        return MathMan.roundInt(getY());
    }

    public int getBlockZ() {
        return MathMan.roundInt(getZ());
    }

    public MutableVector3 setComponents(Vector3 other) {
        return new MutableVector3(other);
    }

    public MutableVector3 setComponents(int x, int y, int z) {
        return new MutableVector3(x, y, z);
    }

    public MutableVector3 setComponents(double x, double y, double z) {
        return new MutableVector3(x, y, z);
    }

    public MutableVector3 mutX(int x) {
        return new MutableVector3(x, getY(), getZ());
    }

    public MutableVector3 mutX(double x) {
        return new MutableVector3(x, getY(), getZ());
    }

    public MutableVector3 mutY(int y) {
        return new MutableVector3(getX(), y, getZ());
    }

    public MutableVector3 mutY(double y) {
        return new MutableVector3(getX(), y, getZ());
    }

    public MutableVector3 mutZ(int z) {
        return new MutableVector3(getX(), getY(), z);
    }

    public MutableVector3 mutZ(double z) {
        return new MutableVector3(getX(), getY(), z);
    }

    /**
     * Get the X coordinate.
     *
     * @return the x coordinate
     */
    public abstract double getX();

    /**
     * Set the X coordinate.
     *
     * @param x the new X
     * @return a new vector
     */
    public Vector3 withX(double x) {
        return Vector3.at(x, getY(), getZ());
    }

    /**
     * Get the Y coordinate.
     *
     * @return the y coordinate
     */
    public abstract double getY();

    /**
     * Set the Y coordinate.
     *
     * @param y the new Y
     * @return a new vector
     */
    public Vector3 withY(double y) {
        return Vector3.at(getX(), y, getZ());
    }

    /**
     * Get the Z coordinate.
     *
     * @return the z coordinate
     */
    public abstract double getZ();

    /**
     * Set the Z coordinate.
     *
     * @param z the new Z
     * @return a new vector
     */
    public Vector3 withZ(double z) {
        return Vector3.at(getX(), getY(), z);
    }

    /**
     * Add another vector to this vector and return the result as a new vector.
     *
     * @param other the other vector
     * @return a new vector
     */
    public Vector3 add(Vector3 other) {
        return add(other.getX(), other.getY(), other.getZ());
    }

    /**
     * Add another vector to this vector and return the result as a new vector.
     *
     * @param x the value to add
     * @param y the value to add
     * @param z the value to add
     * @return a new vector
     */
    public Vector3 add(double x, double y, double z) {
        return Vector3.at(this.getX() + x, this.getY() + y, this.getZ() + z);
    }

    /**
     * Add a list of vectors to this vector and return the
     * result as a new vector.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public Vector3 add(Vector3... others) {
        double newX = getX(), newY = getY(), newZ = getZ();

        for (Vector3 other : others) {
            newX += other.getX();
            newY += other.getY();
            newZ += other.getZ();
        }

        return Vector3.at(newX, newY, newZ);
    }

    /**
     * Subtract another vector from this vector and return the result
     * as a new vector.
     *
     * @param other the other vector
     * @return a new vector
     */
    public Vector3 subtract(Vector3 other) {
        return subtract(other.getX(), other.getY(), other.getZ());
    }

    /**
     * Subtract another vector from this vector and return the result
     * as a new vector.
     *
     * @param x the value to subtract
     * @param y the value to subtract
     * @param z the value to subtract
     * @return a new vector
     */
    public Vector3 subtract(double x, double y, double z) {
        return Vector3.at(this.getX() - x, this.getY() - y, this.getZ() - z);
    }

    /**
     * Subtract a list of vectors from this vector and return the result
     * as a new vector.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public Vector3 subtract(Vector3... others) {
        double newX = getX(), newY = getY(), newZ = getZ();

        for (Vector3 other : others) {
            newX -= other.getX();
            newY -= other.getY();
            newZ -= other.getZ();
        }

        return Vector3.at(newX, newY, newZ);
    }

    /**
     * Multiply this vector by another vector on each component.
     *
     * @param other the other vector
     * @return a new vector
     */
    public Vector3 multiply(Vector3 other) {
        return multiply(other.getX(), other.getY(), other.getZ());
    }

    /**
     * Multiply this vector by another vector on each component.
     *
     * @param x the value to multiply
     * @param y the value to multiply
     * @param z the value to multiply
     * @return a new vector
     */
    public Vector3 multiply(double x, double y, double z) {
        return Vector3.at(this.getX() * x, this.getY() * y, this.getZ() * z);
    }

    /**
     * Multiply this vector by zero or more vectors on each component.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public Vector3 multiply(Vector3... others) {
        double newX = getX(), newY = getY(), newZ = getZ();

        for (Vector3 other : others) {
            newX *= other.getX();
            newY *= other.getY();
            newZ *= other.getZ();
        }

        return Vector3.at(newX, newY, newZ);
    }

    /**
     * Perform scalar multiplication and return a new vector.
     *
     * @param n the value to multiply
     * @return a new vector
     */
    public Vector3 multiply(double n) {
        return multiply(n, n, n);
    }

    /**
     * Divide this vector by another vector on each component.
     *
     * @param other the other vector
     * @return a new vector
     */
    public Vector3 divide(Vector3 other) {
        return divide(other.getX(), other.getY(), other.getZ());
    }

    /**
     * Divide this vector by another vector on each component.
     *
     * @param x the value to divide by
     * @param y the value to divide by
     * @param z the value to divide by
     * @return a new vector
     */
    public Vector3 divide(double x, double y, double z) {
        return Vector3.at(this.getX() / x, this.getY() / y, this.getZ() / z);
    }

    /**
     * Perform scalar division and return a new vector.
     *
     * @param n the value to divide by
     * @return a new vector
     */
    public Vector3 divide(double n) {
        return divide(n, n, n);
    }

    /**
     * Get the length of the vector.
     *
     * @return length
     */
    public double length() {
        return Math.sqrt(lengthSq());
    }

    /**
     * Get the length, squared, of the vector.
     *
     * @return length, squared
     */
    public double lengthSq() {
        return getX() * getX() + getY() * getY() + getZ() * getZ();
    }

    /**
     * Get the distance between this vector and another vector.
     *
     * @param other the other vector
     * @return distance
     */
    public double distance(Vector3 other) {
        return Math.sqrt(distanceSq(other));
    }

    /**
     * Get the distance between this vector and another vector, squared.
     *
     * @param other the other vector
     * @return distance
     */
    public double distanceSq(Vector3 other) {
        double dx = other.getX() - getX();
        double dy = other.getY() - getY();
        double dz = other.getZ() - getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Get the normalized vector, which is the vector divided by its
     * length, as a new vector.
     *
     * @return a new vector
     */
    public Vector3 normalize() {
        return divide(length());
    }

    /**
     * Gets the dot product of this and another vector.
     *
     * @param other the other vector
     * @return the dot product of this and the other vector
     */
    public double dot(Vector3 other) {
        return getX() * other.getX() + getY() * other.getY() + getZ() * other.getZ();
    }

    /**
     * Gets the cross product of this and another vector.
     *
     * @param other the other vector
     * @return the cross product of this and the other vector
     */
    public Vector3 cross(Vector3 other) {
        return Vector3.at(
            getY() * other.getZ() - getZ() * other.getY(),
            getZ() * other.getX() - getX() * other.getZ(),
            getX() * other.getY() - getY() * other.getX()
        );
    }

    /**
     * Checks to see if a vector is contained with another.
     *
     * @param min the minimum point (X, Y, and Z are the lowest)
     * @param max the maximum point (X, Y, and Z are the lowest)
     * @return true if the vector is contained
     */
    public boolean containedWithin(Vector3 min, Vector3 max) {
        return getX() >= min.getX() && getX() <= max.getX() && getY() >= min.getY() && getY() <= max.getY() && getZ() >= min.getZ() && getZ() <= max.getZ();
    }

    /**
     * Clamp the Y component.
     *
     * @param min the minimum value
     * @param max the maximum value
     * @return a new vector
     */
    public Vector3 clampY(int min, int max) {
        checkArgument(min <= max, "minimum cannot be greater than maximum");
        if (getY() < min) {
            return Vector3.at(getX(), min, getZ());
        }
        if (getY() > max) {
            return Vector3.at(getX(), max, getZ());
        }
        return this;
    }

    /**
     * Floors the values of all components.
     *
     * @return a new vector
     */
    public Vector3 floor() {
        return Vector3.at(Math.floor(getX()), Math.floor(getY()), Math.floor(getZ()));
    }

    /**
     * Rounds all components up.
     *
     * @return a new vector
     */
    public Vector3 ceil() {
        return Vector3.at(Math.ceil(getX()), Math.ceil(getY()), Math.ceil(getZ()));
    }

    /**
     * Rounds all components to the closest integer.
     *
     * <p>Components &lt; 0.5 are rounded down, otherwise up.</p>
     *
     * @return a new vector
     */
    public Vector3 round() {
        return Vector3.at(Math.floor(getX() + 0.5), Math.floor(getY() + 0.5), Math.floor(getZ() + 0.5));
    }

    /**
     * Returns a vector with the absolute values of the components of
     * this vector.
     *
     * @return a new vector
     */
    public Vector3 abs() {
        return Vector3.at(Math.abs(getX()), Math.abs(getY()), Math.abs(getZ()));
    }

    /**
     * Perform a 2D transformation on this vector and return a new one.
     *
     * @param angle in degrees
     * @param aboutX about which x coordinate to rotate
     * @param aboutZ about which z coordinate to rotate
     * @param translateX what to add after rotation
     * @param translateZ what to add after rotation
     * @return a new vector
     * @see AffineTransform another method to transform vectors
     */
    public Vector3 transform2D(double angle, double aboutX, double aboutZ, double translateX, double translateZ) {
        angle = Math.toRadians(angle);
        double x = this.getX() - aboutX;
        double z = this.getZ() - aboutZ;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x2 = x * cos - z * sin;
        double z2 = x * sin + z * cos;

        return Vector3.at(
            x2 + aboutX + translateX,
            getY(),
            z2 + aboutZ + translateZ
        );
    }

    /**
     * Get this vector's pitch as used within the game.
     *
     * @return pitch in radians
     */
    public double toPitch() {
        double x = getX();
        double z = getZ();

        if (x == 0 && z == 0) {
            return getY() > 0 ? -90 : 90;
        } else {
            double x2 = x * x;
            double z2 = z * z;
            double xz = Math.sqrt(x2 + z2);
            return Math.toDegrees(Math.atan(-getY() / xz));
        }
    }

    /**
     * Get this vector's yaw as used within the game.
     *
     * @return yaw in radians
     */
    public double toYaw() {
        double x = getX();
        double z = getZ();

        double t = Math.atan2(-x, z);
        double tau = 2 * Math.PI;

        return Math.toDegrees(((t + tau) % tau));
    }

    /**
     * Gets the minimum components of two vectors.
     *
     * @param v2 the second vector
     * @return minimum
     */
    public Vector3 getMinimum(Vector3 v2) {
        return Vector3.at(
                Math.min(getX(), v2.getX()),
                Math.min(getY(), v2.getY()),
                Math.min(getZ(), v2.getZ())
        );
    }

    /**
     * Gets the maximum components of two vectors.
     *
     * @param v2 the second vector
     * @return maximum
     */
    public Vector3 getMaximum(Vector3 v2) {
        return Vector3.at(
                Math.max(getX(), v2.getX()),
                Math.max(getY(), v2.getY()),
                Math.max(getZ(), v2.getZ())
        );
    }

    /**
     * Create a new {@code BlockVector} using the given components.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return a new {@code BlockVector}
     */
    public static BlockVector3 toBlockPoint(double x, double y, double z) {
        return BlockVector3.at(x, y, z);
    }

    /**
     * Create a new {@code BlockVector} from this vector.
     *
     * @return a new {@code BlockVector}
     */
    public BlockVector3 toBlockPoint() {
        return toBlockPoint(getX(), getY(), getZ());
    }

    /**
     * Creates a 2D vector by dropping the Y component from this vector.
     *
     * @return a new {@link Vector2}
     */
    public Vector2 toVector2() {
        return Vector2.at(getX(), getZ());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Vector3)) {
            return false;
        }

        Vector3 other = (Vector3) obj;
        return other.getX() == this.getX() && other.getY() == this.getY() && other.getZ() == this.getZ();
    }

    @Override
    public int hashCode() {
        return (int) getX() ^ (int) getZ() << 12 ^ (int) getY() << 24;
    }

    @Override
    public String toString() {
        String x = (getX() == getBlockX() ? "" + getBlockX() : "" + getX());
        String y = (getY() == getBlockY() ? "" + getBlockY() : "" + getY());
        String z = (getZ() == getBlockZ() ? "" + getBlockZ() : "" + getZ());
        return "(" + x + ", " + y + ", " + z + ")";
    }

}
