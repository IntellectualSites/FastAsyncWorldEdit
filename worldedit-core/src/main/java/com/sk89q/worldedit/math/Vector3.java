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

import com.fastasyncworldedit.core.math.MutableVector3;
import com.fastasyncworldedit.core.math.Vector3Impl;
import com.fastasyncworldedit.core.util.MathMan;
import com.google.common.collect.ComparisonChain;
import com.sk89q.worldedit.math.transform.AffineTransform;

import java.util.Comparator;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * An immutable 3-dimensional vector.
 */
//FAWE start - not a record, make abstract
public abstract class Vector3 {
//FAWE end

    public static final Vector3 ZERO = Vector3.at(0, 0, 0);
    public static final Vector3 UNIT_X = Vector3.at(1, 0, 0);
    public static final Vector3 UNIT_Y = Vector3.at(0, 1, 0);
    public static final Vector3 UNIT_Z = Vector3.at(0, 0, 1);
    public static final Vector3 ONE = Vector3.at(1, 1, 1);

    public static Vector3 at(double x, double y, double z) {
        /*FAWE start - Unnecessary
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
            default:
                break;
        }
        Fawe end
        */
        return new Vector3Impl(x, y, z);
    }

    // thread-safe initialization idiom
    private static final class YzxOrderComparator {

        private static final Comparator<Vector3> YZX_ORDER = (a, b) -> {
            return ComparisonChain.start()
                    .compare(a.y(), b.y())
                    .compare(a.z(), b.z())
                    .compare(a.x(), b.x())
                    .result();
        };

    }

    /**
     * Returns a comparator that sorts vectors first by Y, then Z, then X.
     *
     * <p>
     * Useful for sorting by chunk block storage order.
     * </p>
     */
    public static Comparator<Vector3> sortByCoordsYzx() {
        return YzxOrderComparator.YZX_ORDER;
    }

    //FAWE start

    /**
     * Gets the x coordinate rounded, accounting for negative coordinates
     *
     * @return the x coordinate
     */
    public int getBlockX() {
        return MathMan.roundInt(x());
    }

    /**
     * Gets the y coordinate rounded, accounting for negative coordinates
     *
     * @return the y coordinate
     */
    public int getBlockY() {
        return MathMan.roundInt(y());
    }

    /**
     * Gets the z coordinate rounded, accounting for negative coordinates
     *
     * @return the z coordinate
     */
    public int getBlockZ() {
        return MathMan.roundInt(z());
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
        return new MutableVector3(x, y(), z());
    }

    public MutableVector3 mutX(double x) {
        return new MutableVector3(x, y(), z());
    }

    public MutableVector3 mutY(int y) {
        return new MutableVector3(x(), y, z());
    }

    public MutableVector3 mutY(double y) {
        return new MutableVector3(x(), y, z());
    }

    public MutableVector3 mutZ(int z) {
        return new MutableVector3(x(), y(), z);
    }

    public MutableVector3 mutZ(double z) {
        return new MutableVector3(x(), y(), z);
    }
    //FAWE end

    /**
     * Get the X coordinate.
     *
     * @return the x coordinate
     * @since TODO
     */
    public abstract double x();

    /**
     * Get the X coordinate, aligned to the block grid.
     *
     * @return the block-aligned x coordinate
     */
    public int blockX() {
        return MathMan.roundInt(this.x());
    }

    /**
     * Get the X coordinate.
     *
     * @return the x coordinate
     * @deprecated use {@link #x()} instead
     */
    @Deprecated(forRemoval = true, since = "TODO")
    public double getX() {
        return this.x();
    }

    /**
     * Set the X coordinate.
     *
     * @param x the new X
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 withX(double x) {
        return Vector3.at(x, y(), z());
    }
    //FAWE end


    /**
     * Get the Y coordinate.
     *
     * @return the y coordinate
     * @since TODO
     */
    public abstract double y();

    /**
     * Get the Y coordinate, aligned to the block grid.
     *
     * @return the block-aligned y coordinate
     */
    public int blockY() {
        return MathMan.roundInt(this.y());
    }

    /**
     * Get the Y coordinate.
     *
     * @return the y coordinate
     * @deprecated use {@link #y()} instead
     */
    @Deprecated(forRemoval = true, since = "TODO")
    public double getY() {
        return this.y();
    }

    /**
     * Set the Y coordinate.
     *
     * @param y the new Y
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 withY(double y) {
        return Vector3.at(x(), y, z());
    }
    //FAWE end

    /**
     * Get the Z coordinate.
     *
     * @return the z coordinate
     * @since TODO
     */
    public abstract double z();

    /**
     * Get the Z coordinate, aligned to the block grid.
     *
     * @return the block-aligned z coordinate
     */
    public int blockZ() {
        return MathMan.roundInt(this.z());
    }

    /**
     * Get the Z coordinate.
     *
     * @return the z coordinate
     * @deprecated use {@link #z()} instead
     */
    @Deprecated(forRemoval = true, since = "TODO")
    public double getZ() {
        return this.z();
    }

    /**
     * Set the Z coordinate.
     *
     * @param z the new Z
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 withZ(double z) {
        return Vector3.at(x(), y(), z);
    }
    //FAWE end

    /**
     * Add another vector to this vector and return the result as a new vector.
     *
     * @param other the other vector
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 add(Vector3 other) {
        return add(other.x(), other.y(), other.z());
    }
    //FAWE end

    /**
     * Add another vector to this vector and return the result as a new vector.
     *
     * @param x the value to add
     * @param y the value to add
     * @param z the value to add
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 add(double x, double y, double z) {
        return Vector3.at(this.x() + x, this.y() + y, this.z() + z);
    }
    //FAWE end

    /**
     * Add a list of vectors to this vector and return the
     * result as a new vector.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 add(Vector3... others) {
        double newX = x();
        double newY = y();
        double newZ = z();

        for (Vector3 other : others) {
            newX += other.x();
            newY += other.y();
            newZ += other.z();
        }

        return Vector3.at(newX, newY, newZ);
    }
    //FAWE end

    /**
     * Subtract another vector from this vector and return the result
     * as a new vector.
     *
     * @param other the other vector
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 subtract(Vector3 other) {
        return subtract(other.x(), other.y(), other.z());
    }
    //FAWE end

    /**
     * Subtract another vector from this vector and return the result
     * as a new vector.
     *
     * @param x the value to subtract
     * @param y the value to subtract
     * @param z the value to subtract
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 subtract(double x, double y, double z) {
        return Vector3.at(this.x() - x, this.y() - y, this.z() - z);
    }
    //FAWE end

    /**
     * Subtract a list of vectors from this vector and return the result
     * as a new vector.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 subtract(Vector3... others) {
        double newX = x();
        double newY = y();
        double newZ = z();

        for (Vector3 other : others) {
            newX -= other.x();
            newY -= other.y();
            newZ -= other.z();
        }

        return Vector3.at(newX, newY, newZ);
    }
    //FAWE end

    /**
     * Multiply this vector by another vector on each component.
     *
     * @param other the other vector
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 multiply(Vector3 other) {
        return multiply(other.x(), other.y(), other.z());
    }
    //FAWE end

    /**
     * Multiply this vector by another vector on each component.
     *
     * @param x the value to multiply
     * @param y the value to multiply
     * @param z the value to multiply
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 multiply(double x, double y, double z) {
        return Vector3.at(this.x() * x, this.y() * y, this.z() * z);
    }
    //FAWE end

    /**
     * Multiply this vector by zero or more vectors on each component.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 multiply(Vector3... others) {
        double newX = x();
        double newY = y();
        double newZ = z();

        for (Vector3 other : others) {
            newX *= other.x();
            newY *= other.y();
            newZ *= other.z();
        }

        return Vector3.at(newX, newY, newZ);
    }
    //FAWE end

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
    //FAWE start - getter
    public Vector3 divide(Vector3 other) {
        return divide(other.x(), other.y(), other.z());
    }
    //FAWE end

    /**
     * Divide this vector by another vector on each component.
     *
     * @param x the value to divide by
     * @param y the value to divide by
     * @param z the value to divide by
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 divide(double x, double y, double z) {
        return Vector3.at(this.x() / x, this.y() / y, this.z() / z);
    }
    //FAWE end

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
    //FAWE start - getter
    public double lengthSq() {
        return x() * x() + y() * y() + z() * z();
    }
    //FAWE end

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
    //FAWE start - getter
    public double distanceSq(Vector3 other) {
        double dx = other.x() - x();
        double dy = other.y() - y();
        double dz = other.z() - z();
        return dx * dx + dy * dy + dz * dz;
    }
    //FAWE end

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
    //FAWE start - getter
    public double dot(Vector3 other) {
        return x() * other.x() + y() * other.y() + z() * other.z();
    }
    //FAWE end

    /**
     * Gets the cross product of this and another vector.
     *
     * @param other the other vector
     * @return the cross product of this and the other vector
     */
    //FAWE start - getter
    public Vector3 cross(Vector3 other) {
        return Vector3.at(
                y() * other.z() - z() * other.y(),
                z() * other.x() - x() * other.z(),
                x() * other.y() - y() * other.x()
        );
    }
    //FAWE end

    /**
     * Checks to see if a vector is contained with another.
     *
     * @param min the minimum point (X, Y, and Z are the lowest)
     * @param max the maximum point (X, Y, and Z are the lowest)
     * @return true if the vector is contained
     */
    //FAWE start - getter
    public boolean containedWithin(Vector3 min, Vector3 max) {
        return x() >= min.x() && x() <= max.x() && y() >= min.y() && y() <= max.y() && z() >= min.z() && z() <= max.z();
    }
    //FAWE end

    /**
     * Clamp the Y component.
     *
     * @param min the minimum value
     * @param max the maximum value
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 clampY(int min, int max) {
        checkArgument(min <= max, "minimum cannot be greater than maximum");
        if (y() < min) {
            return Vector3.at(x(), min, z());
        }
        if (y() > max) {
            return Vector3.at(x(), max, z());
        }
        return this;
    }
    //FAWE end

    /**
     * Floors the values of all components.
     *
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 floor() {
        return Vector3.at(Math.floor(x()), Math.floor(y()), Math.floor(z()));
    }
    //FAWE end

    /**
     * Rounds all components up.
     *
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 ceil() {
        return Vector3.at(Math.ceil(x()), Math.ceil(y()), Math.ceil(z()));
    }
    //FAWE end

    /**
     * Rounds all components to the closest integer.
     *
     * <p>Components &lt; 0.5 are rounded down, otherwise up.</p>
     *
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 round() {
        return Vector3.at(Math.floor(x() + 0.5), Math.floor(y() + 0.5), Math.floor(z() + 0.5));
    }
    //FAWE end

    /**
     * Rounds all components using {@link MathUtils#roundHalfUp(double)}
     *
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 roundHalfUp() {
        return Vector3.at(MathUtils.roundHalfUp(x()), MathUtils.roundHalfUp(y()), MathUtils.roundHalfUp(z()));
    }
    //FAWE end

    /**
     * Returns a vector with the absolute values of the components of
     * this vector.
     *
     * @return a new vector
     */
    //FAWE start - getter
    public Vector3 abs() {
        return Vector3.at(Math.abs(x()), Math.abs(y()), Math.abs(z()));
    }
    //FAWE end

    /**
     * Perform a 2D transformation on this vector and return a new one.
     *
     * @param angle      in degrees
     * @param aboutX     about which x coordinate to rotate
     * @param aboutZ     about which z coordinate to rotate
     * @param translateX what to add after rotation
     * @param translateZ what to add after rotation
     * @return a new vector
     * @see AffineTransform another method to transform vectors
     */
    //FAWE start - getter
    public Vector3 transform2D(double angle, double aboutX, double aboutZ, double translateX, double translateZ) {
        angle = Math.toRadians(angle);
        double x = this.x() - aboutX;
        double z = this.z() - aboutZ;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x2 = x * cos - z * sin;
        double z2 = x * sin + z * cos;

        return Vector3.at(
                x2 + aboutX + translateX,
                y(),
                z2 + aboutZ + translateZ
        );
    }
    //FAWE end

    /**
     * Get this vector's pitch as used within the game.
     *
     * @return pitch in radians
     */
    public double toPitch() {
        double x = x();
        double z = z();

        if (x == 0 && z == 0) {
            return y() > 0 ? -90 : 90;
        } else {
            double x2 = x * x;
            double z2 = z * z;
            double xz = Math.sqrt(x2 + z2);
            return Math.toDegrees(Math.atan(-y() / xz));
        }
    }

    /**
     * Get this vector's yaw as used within the game.
     *
     * @return yaw in radians
     */
    public double toYaw() {
        double x = x();
        double z = z();

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
    //FAWE start - getter
    public Vector3 getMinimum(Vector3 v2) {
        return Vector3.at(
                Math.min(x(), v2.x()),
                Math.min(y(), v2.y()),
                Math.min(z(), v2.z())
        );
    }
    //FAWE end

    /**
     * Gets the maximum components of two vectors.
     *
     * @param v2 the second vector
     * @return maximum
     */
    //FAWE start - getter
    public Vector3 getMaximum(Vector3 v2) {
        return Vector3.at(
                Math.max(x(), v2.x()),
                Math.max(y(), v2.y()),
                Math.max(z(), v2.z())
        );
    }
    //FAWE end

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
    //FAWE start - getter
    public BlockVector3 toBlockPoint() {
        return toBlockPoint(x(), y(), z());
    }
    //FAWE end

    /**
     * Creates a 2D vector by dropping the Y component from this vector.
     *
     * @return a new {@link Vector2}
     */
    //FAWE start - getter
    public Vector2 toVector2() {
        return Vector2.at(x(), z());
    }
    //FAWE end

    //FAWE start - not a record, need own implementations
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof final Vector3 other)) {
            return false;
        }

        return other.x() == this.x() && other.y() == this.y() && other.z() == this.z();
    }

    /**
     * Tests if vectors are equal, accounting for floating point errors
     *
     * @param other Another Vector3
     * @return if the vectors are effectively equal
     */
    public boolean equalsFuzzy(Vector3 other) {
        if (this.equals(other)) {
            return true;
        }

        // Minecraft deals in whole blocks, thus any difference smaller than this is unnecessary
        if (Math.abs(x() - other.x()) > 0.000001d) {
            return false;
        }
        if (Math.abs(y() - other.y()) > 0.000001d) {
            return false;
        }
        return !(Math.abs(z() - other.z()) > 0.000001d);
    }

    @Override
    public int hashCode() {
        return (int) x() ^ (int) z() << 12 ^ (int) y() << 24;
    }
    //FAWE end

    @Override
    public String toString() {
        //FAWE start - getter & ternary
        String x = (x() == blockX() ? "" + blockX() : "" + x());
        String y = (y() == blockY() ? "" + blockY() : "" + y());
        String z = (z() == blockZ() ? "" + blockZ() : "" + z());
        //FAWE end
        return "(" + x + ", " + y + ", " + z + ")";
    }

    /**
     * Returns a string representation that is supported by the parser.
     *
     * @return string
     */
    //FAWE start - getter
    public String toParserString() {
        return x() + "," + y() + "," + z();
    }
    //FAWE end

}
