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

import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.math.transform.AffineTransform;
import java.io.IOException;
import java.io.Serializable;
import javax.annotation.Nullable;

/**
 * An immutable 3-dimensional vector.
 */
public class Vector extends Vector2D implements Comparable<Vector>, Serializable {

    public static final Vector ZERO = new Vector(0, 0, 0);
    public static final Vector UNIT_X = new Vector(1, 0, 0);
    public static final Vector UNIT_Y = new Vector(0, 1, 0);
    public static final Vector UNIT_Z = new Vector(0, 0, 1);
    public static final Vector ONE = new Vector(1, 1, 1);

    private transient double y;

    /**
     * Construct an instance.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     */
    public Vector(double x, double y, double z) {
        this.mutX(x);
        this.mutY(y);
        this.mutZ(z);
    }

    /**
     * Construct an instance.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     */
    public Vector(int x, int y, int z) {
        this.mutX(x);
        this.mutY(y);
        this.mutZ(z);
    }

    /**
     * Construct an instance.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     */
    public Vector(float x, float y, float z) {
        this.mutX(x);
        this.mutY(y);
        this.mutZ(z);
    }

    /**
     * Copy another vector.
     *
     * @param other another vector to make a copy of
     */
    public Vector(Vector other) {
        this.mutX(other.getX());
        this.mutY(other.getY());
        this.mutZ(other.getZ());
    }

    public Vector(double[] arr) {
        this.mutX(arr[0]);
        this.mutY(arr[1]);
        this.mutZ(arr[2]);
    }

    /**
     * Construct a new instance with X, Y, and Z coordinates set to 0.
     * <p>
     * <p>One can also refer to a static {@link #ZERO}.</p>
     */
    public Vector() {
        this.mutX(0);
        this.mutY(0);
        this.mutZ(0);
    }

    public Vector setComponents(int x, int y, int z) {
        this.mutX(x);
        this.mutY(y);
        this.mutZ(z);
        return this;
    }

    public Vector setComponents(double x, double y, double z) {
        this.mutX(x);
        this.mutY(y);
        this.mutZ(z);
        return this;
    }

    public void mutX(int x) {
        this.x = x;
    }

    public void mutY(int y) {
        this.y = y;
    }

    public void mutZ(int z) {
        this.z = z;
    }

    public void mutX(double x) {
        this.x = x;
    }

    public void mutY(double y) {
        this.y = y;
    }

    public void mutZ(double z) {
        this.z = z;
    }

    /**
     * Get the X coordinate.
     *
     * @return the x coordinate
     */
    public double getX() {
        return x;
    }

    /**
     * Get the X coordinate rounded.
     *
     * @return the x coordinate
     */
    public int getBlockX() {
        return MathMan.roundInt(getX());
    }

    /**
     * Set the X coordinate.
     *
     * @param x the new X
     * @return a new vector
     */
    public Vector setX(double x) {
        return new Vector(x, getY(), getZ());
    }

    /**
     * Set the X coordinate.
     *
     * @param x the X coordinate
     * @return new vector
     */
    public Vector setX(int x) {
        return new Vector(x, getY(), getZ());
    }

    /**
     * Get the Y coordinate.
     *
     * @return the y coordinate
     */
    public double getY() {
        return y;
    }

    /**
     * Get the Y coordinate rounded.
     *
     * @return the y coordinate
     */
    public int getBlockY() {
        return MathMan.roundInt(getY());
    }

    /**
     * Set the Y coordinate.
     *
     * @param y the new Y
     * @return a new vector
     */
    public Vector setY(double y) {
        return new Vector(getX(), y, getZ());
    }

    /**
     * Set the Y coordinate.
     *
     * @param y the new Y
     * @return a new vector
     */
    public Vector setY(int y) {
        return new Vector(getX(), y, getZ());
    }

    /**
     * Get the Z coordinate.
     *
     * @return the z coordinate
     */
    public double getZ() {
        return z;
    }

    /**
     * Get the Z coordinate rounded.
     *
     * @return the z coordinate
     */
    public int getBlockZ() {
        return MathMan.roundInt(getZ());
    }

    /**
     * Set the Z coordinate.
     *
     * @param z the new Z
     * @return a new vector
     */
    public Vector setZ(double z) {
        return new Vector(getX(), getY(), z);
    }

    /**
     * Set the Z coordinate.
     *
     * @param z the new Z
     * @return a new vector
     */
    public Vector setZ(int z) {
        return new Vector(getX(), getY(), z);
    }

    /**
     * Add another vector to this vector and return the result as a new vector.
     *
     * @param other the other vector
     * @return a new vector
     */
    public Vector add(Vector other) {
        return new Vector(getX() + other.getX(), getY() + other.getY(), getZ() + other.getZ());
    }

    /**
     * Add another vector to this vector and return the result as a new vector.
     *
     * @param x the value to add
     * @param y the value to add
     * @param z the value to add
     * @return a new vector
     */
    public Vector add(double x, double y, double z) {
        return new Vector(this.getX() + x, this.getY() + y, this.getZ() + z);
    }

    /**
     * Add another vector to this vector and return the result as a new vector.
     *
     * @param x the value to add
     * @param y the value to add
     * @param z the value to add
     * @return a new vector
     */
    public Vector add(int x, int y, int z) {
        return new Vector(this.getX() + x, this.getY() + y, this.getZ() + z);
    }

    /**
     * Add a list of vectors to this vector and return the
     * result as a new vector.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public Vector add(Vector... others) {
        double newX = getX(), newY = getY(), newZ = getZ();

        for (Vector other : others) {
            newX += other.getX();
            newY += other.getY();
            newZ += other.getZ();
        }

        return new Vector(newX, newY, newZ);
    }

    /**
     * Subtract another vector from this vector and return the result
     * as a new vector.
     *
     * @param other the other vector
     * @return a new vector
     */
    public Vector subtract(Vector other) {
        return new Vector(getX() - other.getX(), getY() - other.getY(), getZ() - other.getZ());
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
    public Vector subtract(double x, double y, double z) {
        return new Vector(this.getX() - x, this.getY() - y, this.getZ() - z);
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
    public Vector subtract(int x, int y, int z) {
        return new Vector(this.getX() - x, this.getY() - y, this.getZ() - z);
    }

    /**
     * Subtract a list of vectors from this vector and return the result
     * as a new vector.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public Vector subtract(Vector... others) {
        double newX = getX(), newY = getY(), newZ = getZ();

        for (Vector other : others) {
            newX -= other.getX();
            newY -= other.getY();
            newZ -= other.getZ();
        }

        return new Vector(newX, newY, newZ);
    }

    /**
     * Multiply this vector by another vector on each component.
     *
     * @param other the other vector
     * @return a new vector
     */
    public Vector multiply(Vector other) {
        return new Vector(getX() * other.getX(), getY() * other.getY(), getZ() * other.getZ());
    }

    /**
     * Multiply this vector by another vector on each component.
     *
     * @param x the value to multiply
     * @param y the value to multiply
     * @param z the value to multiply
     * @return a new vector
     */
    public Vector multiply(double x, double y, double z) {
        return new Vector(this.getX() * x, this.getY() * y, this.getZ() * z);
    }

    /**
     * Multiply this vector by another vector on each component.
     *
     * @param x the value to multiply
     * @param y the value to multiply
     * @param z the value to multiply
     * @return a new vector
     */
    public Vector multiply(int x, int y, int z) {
        return new Vector(this.getX() * x, this.getY() * y, this.getZ() * z);
    }

    /**
     * Multiply this vector by zero or more vectors on each component.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public Vector multiply(Vector... others) {
        double newX = getX(), newY = getY(), newZ = getZ();

        for (Vector other : others) {
            newX *= other.getX();
            newY *= other.getY();
            newZ *= other.getZ();
        }

        return new Vector(newX, newY, newZ);
    }

    /**
     * Perform scalar multiplication and return a new vector.
     *
     * @param n the value to multiply
     * @return a new vector
     */
    public Vector multiply(double n) {
        return new Vector(this.getX() * n, this.getY() * n, this.getZ() * n);
    }

    /**
     * Perform scalar multiplication and return a new vector.
     *
     * @param n the value to multiply
     * @return a new vector
     */
    public Vector multiply(float n) {
        return new Vector(this.getX() * n, this.getY() * n, this.getZ() * n);
    }

    /**
     * Perform scalar multiplication and return a new vector.
     *
     * @param n the value to multiply
     * @return a new vector
     */
    public Vector multiply(int n) {
        return new Vector(this.getX() * n, this.getY() * n, this.getZ() * n);
    }

    /**
     * Divide this vector by another vector on each component.
     *
     * @param other the other vector
     * @return a new vector
     */
    public Vector divide(Vector other) {
        return new Vector(getX() / other.getX(), getY() / other.getY(), getZ() / other.getZ());
    }

    /**
     * Divide this vector by another vector on each component.
     *
     * @param x the value to divide by
     * @param y the value to divide by
     * @param z the value to divide by
     * @return a new vector
     */
    public Vector divide(double x, double y, double z) {
        return new Vector(this.getX() / x, this.getY() / y, this.getZ() / z);
    }

    /**
     * Divide this vector by another vector on each component.
     *
     * @param x the value to divide by
     * @param y the value to divide by
     * @param z the value to divide by
     * @return a new vector
     */
    public Vector divide(int x, int y, int z) {
        return new Vector(this.getX() / x, this.getY() / y, this.getZ() / z);
    }

    /**
     * Perform scalar division and return a new vector.
     *
     * @param n the value to divide by
     * @return a new vector
     */
    public Vector divide(int n) {
        return new Vector(getX() / n, getY() / n, getZ() / n);
    }

    /**
     * Perform scalar division and return a new vector.
     *
     * @param n the value to divide by
     * @return a new vector
     */
    public Vector divide(double n) {
        return new Vector(getX() / n, getY() / n, getZ() / n);
    }

    /**
     * Perform scalar division and return a new vector.
     *
     * @param n the value to divide by
     * @return a new vector
     */
    public Vector divide(float n) {
        return new Vector(getX() / n, getY() / n, getZ() / n);
    }

    /**
     * Get the length of the vector.
     *
     * @return length
     */
    public double length() {
        return Math.sqrt(getX() * getX() + getY() * getY() + getZ() * getZ());
    }

    /**
     * Get the length, squared, of the vector.
     *
     * @return length, squared
     */
    public double lengthSq() {
        return getX() * getX() + getY() * getY() + getZ() * getZ();
    }

    public double volume() {
        return getX() * getY() * getZ();
    }

    /**
     * Get the distance between this vector and another vector.
     *
     * @param other the other vector
     * @return distance
     */
    public double distance(Vector other) {
        return Math.sqrt(Math.pow(other.getX() - getX(), 2) +
                Math.pow(other.getY() - getY(), 2) +
                Math.pow(other.getZ() - getZ(), 2));
    }

    /**
     * Get the distance between this vector and another vector, squared.
     *
     * @param other the other vector
     * @return distance
     */
    public double distanceSq(Vector other) {
        return Math.pow(other.getX() - getX(), 2) +
                Math.pow(other.getY() - getY(), 2) +
                Math.pow(other.getZ() - getZ(), 2);
    }

    /**
     * Get the normalized vector, which is the vector divided by its
     * length, as a new vector.
     *
     * @return a new vector
     */
    public Vector normalize() {
        return divide(length());
    }

    /**
     * Gets the dot product of this and another vector.
     *
     * @param other the other vector
     * @return the dot product of this and the other vector
     */
    public double dot(Vector other) {
        return getX() * other.getX() + getY() * other.getY() + getZ() * other.getZ();
    }

    /**
     * Gets the cross product of this and another vector.
     *
     * @param other the other vector
     * @return the cross product of this and the other vector
     */
    public Vector cross(Vector other) {
        return new Vector(
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
    public boolean containedWithin(Vector min, Vector max) {
        return getX() >= min.getX() && getX() <= max.getX() && getY() >= min.getY() && getY() <= max.getY() && getZ() >= min.getZ() && getZ() <= max.getZ();
    }

    /**
     * Checks to see if a vector is contained with another, comparing
     * using discrete comparisons, inclusively.
     *
     * @param min the minimum point (X, Y, and Z are the lowest)
     * @param max the maximum point (X, Y, and Z are the lowest)
     * @return true if the vector is contained
     */
    public boolean containedWithinBlock(Vector min, Vector max) {
        return getBlockX() >= min.getBlockX() && getBlockX() <= max.getBlockX()
                && getBlockY() >= min.getBlockY() && getBlockY() <= max.getBlockY()
                && getBlockZ() >= min.getBlockZ() && getBlockZ() <= max.getBlockZ();
    }

    /**
     * Clamp the Y component.
     *
     * @param min the minimum value
     * @param max the maximum value
     * @return a new vector
     */
    public Vector clampY(int min, int max) {
        return new Vector(getX(), Math.max(min, Math.min(max, getY())), getZ());
    }

    /**
     * Floors the values of all components.
     *
     * @return a new vector
     */
    public Vector floor() {
        return new Vector(Math.floor(getX()), Math.floor(getY()), Math.floor(getZ()));
    }

    /**
     * Rounds all components up.
     *
     * @return a new vector
     */
    public Vector ceil() {
        return new Vector(Math.ceil(getX()), Math.ceil(getY()), Math.ceil(getZ()));
    }

    /**
     * Rounds all components to the closest integer.
     * <p>
     * <p>Components &lt; 0.5 are rounded down, otherwise up.</p>
     *
     * @return a new vector
     */
    public Vector round() {
        return new Vector(Math.floor(getX() + 0.5), Math.floor(getY() + 0.5), Math.floor(getZ() + 0.5));
    }

    /**
     * Returns a vector with the absolute values of the components of
     * this vector.
     *
     * @return a new vector
     */
    public Vector positive() {
        return new Vector(Math.abs(getX()), Math.abs(getY()), Math.abs(getZ()));
    }

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
    public Vector transform2D(double angle, double aboutX, double aboutZ, double translateX, double translateZ) {
        angle = Math.toRadians(angle);
        double x = this.getX() - aboutX;
        double z = this.getZ() - aboutZ;
        double x2 = x * Math.cos(angle) - z * Math.sin(angle);
        double z2 = x * Math.sin(angle) + z * Math.cos(angle);

        return new Vector(
                x2 + aboutX + translateX,
                getY(),
                z2 + aboutZ + translateZ
        );
    }

    /**
     * Returns whether this vector is collinear with another vector.
     *
     * @param other the other vector
     * @return true if collinear
     */
    public boolean isCollinearWith(Vector other) {
        if (getX() == 0 && getY() == 0 && getZ() == 0) {
            // this is a zero vector
            return true;
        }

        final double otherX = other.getX();
        final double otherY = other.getY();
        final double otherZ = other.getZ();

        if (otherX == 0 && otherY == 0 && otherZ == 0) {
            // other is a zero vector
            return true;
        }

        if ((getX() == 0) != (otherX == 0)) return false;
        if ((getY() == 0) != (otherY == 0)) return false;
        if ((getZ() == 0) != (otherZ == 0)) return false;

        final double quotientX = otherX / getX();
        if (!Double.isNaN(quotientX)) {
            return other.equals(multiply(quotientX));
        }

        final double quotientY = otherY / getY();
        if (!Double.isNaN(quotientY)) {
            return other.equals(multiply(quotientY));
        }

        final double quotientZ = otherZ / getZ();
        if (!Double.isNaN(quotientZ)) {
            return other.equals(multiply(quotientZ));
        }

        throw new RuntimeException("This should not happen");
    }

    /**
     * Get this vector's pitch as used within the game.
     *
     * @return pitch in radians
     */
    public float toPitch() {
        double x = getX();
        double z = getZ();

        if (x == 0 && z == 0) {
            return getY() > 0 ? -90 : 90;
        } else {
            double x2 = x * x;
            double z2 = z * z;
            double xz = Math.sqrt(x2 + z2);
            return (float) Math.toDegrees(Math.atan(-getY() / xz));
        }
    }

    /**
     * Get this vector's yaw as used within the game.
     *
     * @return yaw in radians
     */
    public float toYaw() {
        double x = getX();
        double z = getZ();

        double t = Math.atan2(-x, z);
        double _2pi = 2 * Math.PI;

        return (float) Math.toDegrees(((t + _2pi) % _2pi));
    }

    /**
     * Create a new {@code BlockVector} using the given components.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return a new {@code BlockVector}
     */
    public static BlockVector toBlockPoint(double x, double y, double z) {
        return new BlockVector(
                Math.floor(x),
                Math.floor(y),
                Math.floor(z)
        );
    }

    /**
     * Create a new {@code BlockVector} from this vector.
     *
     * @return a new {@code BlockVector}
     */
    public BlockVector toBlockPoint() {
        return new BlockVector(
                Math.floor(getX()),
                Math.floor(getY()),
                Math.floor(getZ())
        );
    }

    /**
     * Create a new {@code BlockVector} from this vector.
     *
     * @return a new {@code BlockVector}
     */
    public BlockVector toBlockVector() {
        return new BlockVector(this);
    }

    /**
     * Creates a 2D vector by dropping the Y component from this vector.
     *
     * @return a new {@code Vector2D}
     */
    public Vector2D toVector2D() {
        return new Vector2D(getX(), getZ());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Vector)) {
            return false;
        }

        Vector other = (Vector) obj;
        return other.getX() == this.getX() && other.getZ() == this.getZ() && other.getY() == this.getY();
    }

    @Override
    public int compareTo(@Nullable Vector other) {
        if (other == null) {
            throw new IllegalArgumentException("null not supported");
        }
        if (getY() != other.getY()) return Double.compare(getY(), other.getY());
        if (getZ() != other.getZ()) return Double.compare(getZ(), other.getZ());
        if (getX() != other.getX()) return Double.compare(getX(), other.getX());
        return 0;
    }

    @Override
    public int hashCode() {
        return ((int) getX() ^ ((int) getZ() << 16)) ^ ((int) getY() << 30);
    }

    @Override
    public String toString() {
        String x = (getX() == getBlockX() ? "" + getBlockX() : "" + getX());
        String y = (getY() == getBlockY() ? "" + getBlockY() : "" + getY());
        String z = (getZ() == getBlockZ() ? "" + getBlockZ() : "" + getZ());
        return "(" + x + ", " + y + ", " + z + ")";
    }

    /**
     * Gets the minimum components of two vectors.
     *
     * @param v1 the first vector
     * @param v2 the second vector
     * @return minimum
     */
    public static Vector getMinimum(Vector v1, Vector v2) {
        return new Vector(
                Math.min(v1.getX(), v2.getX()),
                Math.min(v1.getY(), v2.getY()),
                Math.min(v1.getZ(), v2.getZ())
        );
    }

    /**
     * Gets the maximum components of two vectors.
     *
     * @param v1 the first vector
     * @param v2 the second vector
     * @return maximum
     */
    public static Vector getMaximum(Vector v1, Vector v2) {
        return new Vector(
                Math.max(v1.getX(), v2.getX()),
                Math.max(v1.getY(), v2.getY()),
                Math.max(v1.getZ(), v2.getZ())
        );
    }
    
    /**
     * Gets the midpoint of two vectors.
     *
     * @param v1 the first vector
     * @param v2 the second vector
     * @return maximum
     */
    public static Vector getMidpoint(Vector v1, Vector v2) {
        return new Vector(
                (v1.getX() + v2.getX()) / 2,
                (v1.getY() + v2.getY()) / 2,
                (v1.getZ() + v2.getZ()) / 2
        );
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        if (!(this instanceof MutableBlockVector)) {
            stream.writeDouble(x);
            stream.writeDouble(y);
            stream.writeDouble(z);
        }
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        if (this instanceof MutableBlockVector) return;
        this.x = stream.readDouble();
        this.y = stream.readDouble();
        this.z = stream.readDouble();
    }


}
