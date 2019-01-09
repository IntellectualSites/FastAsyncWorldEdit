package com.sk89q.worldedit.math;

import com.sk89q.worldedit.math.transform.AffineTransform;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.io.Serializable;

/**
 * A mutable rendition of WorldEdit's Vector3 class.
 * This class should ONLY be used locally for efficient vector manipulation.
 */

public class MutableVector implements Serializable {
    private transient double x, y, z;

    private static ThreadLocal<MutableVector> MUTABLE_CACHE = new ThreadLocal<MutableVector>() {
        @Override
        protected MutableVector initialValue() {
            return new MutableVector();
        }
    };

    public static MutableVector get(double x, double y, double z) {
        return MUTABLE_CACHE.get().setComponents(x, y, z);
    }

    public MutableVector(Vector3 v) {
        this(v.getX(), v.getY(), v.getZ());
    }

    public MutableVector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public MutableVector() {
        this(0, 0, 0);
    }

    public MutableVector setComponents(Vector3 other) {
        return setComponents(other.getX(), other.getY(), other.getZ());
    }
    
    public MutableVector setComponents(MutableVector mutable) {
    	return setComponents(mutable.getX(), mutable.getY(), mutable.getZ());
    }

    public MutableVector setComponents(int x, int y, int z) {
        return this.setComponents((double) x, (double) y, (double) z);
    }

    public MutableVector setComponents(double x, double y, double z) {
        this.mutX(x);
        this.mutY(y);
        this.mutZ(z);
        return this;
    }

    public final void mutX(double x) {
        this.x = y;
    }

    public final void mutY(double y) {
        this.y = y;
    }

    public final void mutZ(double z) {
        this.z = y;
    }

    public final void mutX(int x) {
        this.x = (double)x;
    }

    public final void mutY(int y) {
        this.y = (double)y;
    }

    public final void mutZ(int z) {
        this.z = (double)z;
    }

    public final double getX() {
        return x;
    }

    public final double getY() {
        return y;
    }

    public final double getZ() {
        return z;
    }
    
    /**
     * Add another vector to this vector and return the result as a new vector.
     *
     * @param other the other vector
     * @return a new vector
     */
    public MutableVector add(MutableVector other) {
        return add(other.x, other.y, other.z);
    }

    /**
     * Add another vector to this vector and return the result as a new vector.
     *
     * @param x the value to add
     * @param y the value to add
     * @param z the value to add
     * @return a new vector
     */
    public MutableVector add(double x, double y, double z) {
    	this.x += x;
    	this.y += y;
    	this.z += z;
        return this;
    }

    /**
     * Add a list of vectors to this vector and return the
     * result as a new vector.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public MutableVector add(MutableVector... others) {

        for (MutableVector other : others) {
            x += other.x;
            y += other.y;
            z += other.z;
        }

        return this;
    }

    /**
     * Subtract another vector from this vector and return the result
     * as a new vector.
     *
     * @param other the other vector
     * @return a new vector
     */
    public MutableVector subtract(MutableVector other) {
        return subtract(other.x, other.y, other.z);
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
    public MutableVector subtract(double x, double y, double z) {
    	this.x -= x;
    	this.y -= y;
    	this.z -= z;
        return this;
    }

    /**
     * Subtract a list of vectors from this vector and return the result
     * as a new vector.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public MutableVector subtract(MutableVector... others) {

        for (MutableVector other : others) {
            x -= other.x;
            y -= other.y;
            z -= other.z;
        }

        return this;
    }

    /**
     * Multiply this vector by another vector on each component.
     *
     * @param other the other vector
     * @return a new vector
     */
    public MutableVector multiply(MutableVector other) {
        return multiply(other.x, other.y, other.z);
    }

    /**
     * Multiply this vector by another vector on each component.
     *
     * @param x the value to multiply
     * @param y the value to multiply
     * @param z the value to multiply
     * @return a new vector
     */
    public MutableVector multiply(double x, double y, double z) {
    	this.x *= x;
    	this.y *= y;
    	this.z *= z;
        return this;
    }

    /**
     * Multiply this vector by zero or more vectors on each component.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public MutableVector multiply(MutableVector... others) {

        for (MutableVector other : others) {
            x *= other.x;
            y *= other.y;
            z *= other.z;
        }

        return this;
    }

    /**
     * Perform scalar multiplication and return a new vector.
     *
     * @param n the value to multiply
     * @return a new vector
     */
    public MutableVector multiply(double n) {
        return multiply(n, n, n);
    }

    /**
     * Divide this vector by another vector on each component.
     *
     * @param other the other vector
     * @return a new vector
     */
    public MutableVector divide(MutableVector other) {
        return divide(other.x, other.y, other.z);
    }

    /**
     * Divide this vector by another vector on each component.
     *
     * @param x the value to divide by
     * @param y the value to divide by
     * @param z the value to divide by
     * @return a new vector
     */
    public MutableVector divide(double x, double y, double z) {
    	this.x /= x;
    	this.y /= y;
    	this.z /= z;
        return this;
    }

    /**
     * Perform scalar division and return a new vector.
     *
     * @param n the value to divide by
     * @return a new vector
     */
    public MutableVector divide(double n) {
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
        return x * x + y * y + z * z;
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
        double dx = other.getX() - x;
        double dy = other.getY() - y;
        double dz = other.getZ() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Get the normalized vector, which is the vector divided by its
     * length, as a new vector.
     *
     * @return a new vector
     */
    public MutableVector normalize() {
        return divide(length());
    }

    /**
     * Gets the dot product of this and another vector.
     *
     * @param other the other vector
     * @return the dot product of this and the other vector
     */
    public double dot(MutableVector other) {
        return x * other.x + y * other.y + z * other.z;
    }

    /**
     * Gets the cross product of this and another vector.
     *
     * @param other the other vector
     * @return the cross product of this and the other vector
     */
    public Vector3 cross(MutableVector other) {
        return new Vector3(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        );
    }

    /**
     * Checks to see if a vector is contained with another.
     *
     * @param min the minimum point (X, Y, and Z are the lowest)
     * @param max the maximum point (X, Y, and Z are the lowest)
     * @return true if the vector is contained
     */
    public boolean containedWithin(MutableVector min, MutableVector max) {
        return x >= min.x && x <= max.x && y >= min.y && y <= max.y && z >= min.z && z <= max.z;
    }

    /**
     * Clamp the Y component.
     *
     * @param min the minimum value
     * @param max the maximum value
     * @return a new vector
     */
    public MutableVector clampY(int min, int max) {
        checkArgument(min <= max, "minimum cannot be greater than maximum");
        if (y < min) {
            y = min;
        }
        if (y > max) {
            y = max;
        }
        return this;
    }

    /**
     * Floors the values of all components.
     *
     * @return a new vector
     */
    public MutableVector floor() {
    	x = Math.floor(x);
    	y = Math.floor(y);
    	z = Math.floor(z);
        return this;
    }

    /**
     * Rounds all components up.
     *
     * @return a new vector
     */
    public MutableVector ceil() {
    	x = Math.ceil(x);
    	y = Math.ceil(y);
    	z = Math.ceil(z);
        return this;
    }

    /**
     * Rounds all components to the closest integer.
     *
     * <p>Components &lt; 0.5 are rounded down, otherwise up.</p>
     *
     * @return a new vector
     */
    public MutableVector round() {
    	x = Math.floor(x + 0.5);
    	y = Math.floor(y + 0.5);
    	z = Math.floor(z + 0.5);
        return this;
    }

    /**
     * Returns a vector with the absolute values of the components of
     * this vector.
     *
     * @return a new vector
     */
    public MutableVector abs() {
    	x = Math.abs(x);
    	y = Math.abs(y);
    	z = Math.abs(z);
        return this;
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
    public MutableVector transform2D(double angle, double aboutX, double aboutZ, double translateX, double translateZ) {
        angle = Math.toRadians(angle);
        double x = this.x - aboutX;
        double z = this.z - aboutZ;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x2 = x * cos - z * sin;
        double z2 = x * sin + z * cos;
        this.x = x2 + aboutX + translateX;
        this.z = z2 + aboutZ + translateZ;
        return this;
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
    public MutableVector getMinimum(MutableVector v2) {
    	x = Math.min(x, v2.x);
    	y = Math.min(y, v2.y);
    	z = Math.min(z, v2.z);
        return this;
    }

    /**
     * Gets the maximum components of two vectors.
     *
     * @param v2 the second vector
     * @return maximum
     */
    public MutableVector getMaximum(MutableVector v2) {
    	x = Math.max(x, v2.x);
    	y = Math.max(y, v2.y);
    	z = Math.max(z, v2.z);
        return this;
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
        return new BlockVector3(x, y, z);
    }

    /**
     * Create a new {@code BlockVector} from this vector.
     *
     * @return a new {@code BlockVector}
     */
    public BlockVector3 toBlockPoint() {
        return toBlockPoint(x, y, z);
    }

    /**
     * Creates a 2D vector by dropping the Y component from this vector.
     *
     * @return a new {@link Vector2}
     */
    public Vector2 toVector2() {
        return new Vector2(x, z);
    }
    
    public Vector3 toVector3() {
    	return new Vector3(x, y, z);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MutableVector)) {
            return false;
        }

        MutableVector other = (MutableVector) obj;
        return other.x == this.x && other.y == this.y && other.z == this.z;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + Double.hashCode(x);
        hash = 31 * hash + Double.hashCode(y);
        hash = 31 * hash + Double.hashCode(z);
        return hash;
    }

    @Override
    public String toString() {
        return "Mutable (" + x + ", " + y + ", " + z + ")";
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.writeDouble(x);
        stream.writeByte((byte) y);
        stream.writeDouble(z);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        this.x = stream.readDouble();
        this.y = stream.readByte() & 0xFF;
        this.z = stream.readDouble();
    }
}