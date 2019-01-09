package com.sk89q.worldedit.math;

import com.boydti.fawe.util.MathMan;
import com.google.common.collect.ComparisonChain;
import com.sk89q.worldedit.math.transform.AffineTransform;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;

/**
 * A mutable rendition of WorldEdit's BlockVector3 class.
 * This class should ONLY be used locally for efficient vector manipulation.
 */

public class MutableBlockVector implements Serializable {
    private transient int x, y, z;

    private static ThreadLocal<MutableBlockVector> MUTABLE_CACHE = new ThreadLocal<MutableBlockVector>() {
        @Override
        protected MutableBlockVector initialValue() {
            return new MutableBlockVector();
        }
    };

    public static MutableBlockVector get(int x, int y, int z) {
        return MUTABLE_CACHE.get().setComponents(x, y, z);
    }

    public MutableBlockVector(BlockVector3 v) {
        this(v.getBlockX(), v.getBlockY(), v.getBlockZ());
    }

    public MutableBlockVector(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public MutableBlockVector() {
        this(0, 0, 0);
    }
    
    public static final MutableBlockVector ZERO = new MutableBlockVector(0, 0, 0);
    public static final MutableBlockVector UNIT_X = new MutableBlockVector(1, 0, 0);
    public static final MutableBlockVector UNIT_Y = new MutableBlockVector(0, 1, 0);
    public static final MutableBlockVector UNIT_Z = new MutableBlockVector(0, 0, 1);
    public static final MutableBlockVector ONE = new MutableBlockVector(1, 1, 1);

    // thread-safe initialization idiom
    private static final class YzxOrderComparator {
        private static final Comparator<MutableBlockVector> YZX_ORDER = (a, b) -> {
            return ComparisonChain.start()
                    .compare(a.y, b.y)
                    .compare(a.z, b.z)
                    .compare(a.x, b.x)
                    .result();
        };
    }

    /**
     * Returns a comparator that sorts vectors first by Y, then Z, then X.
     * 
     * <p>
     * Useful for sorting by chunk block storage order.
     */
    public static Comparator<MutableBlockVector> sortByCoordsYzx() {
        return YzxOrderComparator.YZX_ORDER;
    }

    public MutableBlockVector setComponents(BlockVector3 other) {
        return setComponents(other.getBlockX(), other.getBlockY(), other.getBlockZ());
    }

    public MutableBlockVector setComponents(double x, double y, double z) {
        return this.setComponents((int) x, (int) y, (int) z);
    }

    public MutableBlockVector setComponents(int x, int y, int z) {
        this.mutX(x);
        this.mutY(y);
        this.mutZ(z);
        return this;
    }

    public final void mutX(double x) {
        this.x = MathMan.roundInt(x);
    }

    public final void mutY(double y) {
        this.y = MathMan.roundInt(y);
    }

    public final void mutZ(double z) {
        this.z = MathMan.roundInt(z);
    }

    public final void mutX(int x) {
        this.x = x;
    }

    public final void mutY(int y) {
        this.y = y;
    }

    public final void mutZ(int z) {
        this.z = z;
    }

    /**
     * Get the X coordinate.
     *
     * @return the x coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * Get the X coordinate.
     *
     * @return the x coordinate
     */
    public int getBlockX() {
        return x;
    }
    
    /**
     * Get the Y coordinate.
     *
     * @return the y coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * Get the Y coordinate.
     *
     * @return the y coordinate
     */
    public int getBlockY() {
        return y;
    }

    /**
     * Get the Z coordinate.
     *
     * @return the z coordinate
     */
    public int getZ() {
        return z;
    }

    /**
     * Get the Z coordinate.
     *
     * @return the z coordinate
     */
    public int getBlockZ() {
        return z;
    }
    
    /**
     * Add another vector to this vector.
     *
     * @param other the other vector
     * @return a new vector
     */
    public MutableBlockVector add(MutableBlockVector other) {
        return add(other.x, other.y, other.z);
    }

    /**
     * Add another vector to this vector.
     *
     * @param x the value to add
     * @param y the value to add
     * @param z the value to add
     * @return a new vector
     */
    public MutableBlockVector add(int x, int y, int z) {
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
    public MutableBlockVector add(MutableBlockVector... others) {

        for (MutableBlockVector other : others) {
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
    public MutableBlockVector subtract(MutableBlockVector other) {
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
    public MutableBlockVector subtract(int x, int y, int z) {
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
    public MutableBlockVector subtract(MutableBlockVector... others) {

        for (MutableBlockVector other : others) {
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
    public MutableBlockVector multiply(MutableBlockVector other) {
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
    public MutableBlockVector multiply(int x, int y, int z) {
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
    public MutableBlockVector multiply(MutableBlockVector... others) {

        for (MutableBlockVector other : others) {
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
    public MutableBlockVector multiply(int n) {
        return multiply(n, n, n);
    }

    /**
     * Divide this vector by another vector on each component.
     *
     * @param other the other vector
     * @return a new vector
     */
    public MutableBlockVector divide(MutableBlockVector other) {
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
    public MutableBlockVector divide(int x, int y, int z) {
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
    public MutableBlockVector divide(int n) {
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
    public int lengthSq() {
        return x * x + y * y + z * z;
    }

    /**
     * Get the distance between this vector and another vector.
     *
     * @param other the other vector
     * @return distance
     */
    public double distance(MutableBlockVector other) {
        return Math.sqrt(distanceSq(other));
    }

    /**
     * Get the distance between this vector and another vector, squared.
     *
     * @param other the other vector
     * @return distance
     */
    public int distanceSq(MutableBlockVector other) {
        int dx = other.x - x;
        int dy = other.y - y;
        int dz = other.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Get the normalized vector, which is the vector divided by its
     * length, as a new vector.
     *
     * @return a new vector
     */
    public MutableBlockVector normalize() {
        double len = length();
        x /= len;
        y /= len;
        z /= len;
        return this;
    }

    /**
     * Gets the dot product of this and another vector.
     *
     * @param other the other vector
     * @return the dot product of this and the other vector
     */
    public double dot(MutableBlockVector other) {
        return x * other.x + y * other.y + z * other.z;
    }

    /**
     * Gets the cross product of this and another vector.
     *
     * @param other the other vector
     * @return the cross product of this and the other vector
     */
    public MutableBlockVector cross(MutableBlockVector other) {
    	x = y * other.z - z * other.y;
    	y = z * other.x - x * other.z;
    	z = x * other.y - y * other.x;
        return this;
    }

    /**
     * Checks to see if a vector is contained with another.
     *
     * @param min the minimum point (X, Y, and Z are the lowest)
     * @param max the maximum point (X, Y, and Z are the lowest)
     * @return true if the vector is contained
     */
    public boolean containedWithin(MutableBlockVector min, MutableBlockVector max) {
        return x >= min.x && x <= max.x && y >= min.y && y <= max.y && z >= min.z && z <= max.z;
    }

    /**
     * Clamp the Y component.
     *
     * @param min the minimum value
     * @param max the maximum value
     * @return a new vector
     */
    public MutableBlockVector clampY(int min, int max) {
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
    public MutableBlockVector floor() {
        // already floored, kept for feature parity with Vector3
        return this;
    }

    /**
     * Rounds all components up.
     *
     * @return a new vector
     */
    public MutableBlockVector ceil() {
        // already raised, kept for feature parity with Vector3
        return this;
    }

    /**
     * Rounds all components to the closest integer.
     *
     * <p>Components &lt; 0.5 are rounded down, otherwise up.</p>
     *
     * @return a new vector
     */
    public MutableBlockVector round() {
        // already rounded, kept for feature parity with Vector3
        return this;
    }

    /**
     * Returns a vector with the absolute values of the components of
     * this vector.
     *
     * @return a new vector
     */
    public MutableBlockVector abs() {
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
    public MutableBlockVector transform2D(double angle, double aboutX, double aboutZ, double translateX, double translateZ) {
        angle = Math.toRadians(angle);
        double x = this.x - aboutX;
        double z = this.z - aboutZ;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x2 = x * cos - z * sin;
        double z2 = x * sin + z * cos;
        this.x = (int) Math.floor(x2 + aboutX + translateX);
        this.z = (int) Math.floor(z2 + aboutZ + translateZ);
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
    public MutableBlockVector getMinimum(MutableBlockVector v2) {
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
    public MutableBlockVector getMaximum(MutableBlockVector v2) {
    	x = Math.max(x, v2.x);
    	y = Math.max(y, v2.y);
    	z = Math.max(z, v2.z);
    	return this;
    }

    /**
     * Creates a 2D vector by dropping the Y component from this vector.
     *
     * @return a new {@link BlockVector2}
     */
    public BlockVector2 toBlockVector2() {
        return new BlockVector2(x, z);
    }

    public Vector3 toVector3() {
        return new Vector3(x, y, z);
    }
    
    public BlockVector3 toBlockVector3() {
    	return new BlockVector3(x, y, z);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MutableBlockVector)) {
            return false;
        }

        MutableBlockVector other = (MutableBlockVector) obj;
        return other.x == this.x && other.y == this.y && other.z == this.z;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + Integer.hashCode(x);
        hash = 31 * hash + Integer.hashCode(y);
        hash = 31 * hash + Integer.hashCode(z);
        return hash;
    }

    @Override
    public String toString() {
        return "Mutable (" + x + ", " + y + ", " + z + ")";
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.writeInt(x);
        stream.writeByte((byte) y);
        stream.writeInt(z);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        this.x = stream.readInt();
        this.y = stream.readByte() & 0xFF;
        this.z = stream.readInt();
    }
}