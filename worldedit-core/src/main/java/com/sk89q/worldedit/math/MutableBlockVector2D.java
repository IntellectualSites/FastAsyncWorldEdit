package com.sk89q.worldedit.math;

import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;

import com.google.common.collect.ComparisonChain;
import com.sk89q.worldedit.math.transform.AffineTransform;

/**
 * A mutable rendition of WorldEdit's BlockVector2 class.
 * This class should ONLY be used locally for efficient vector manipulation.
 */
public final class MutableBlockVector2D implements Serializable {
    private static ThreadLocal<MutableBlockVector2D> MUTABLE_CACHE = new ThreadLocal<MutableBlockVector2D>() {
        @Override
        protected MutableBlockVector2D initialValue() {
            return new MutableBlockVector2D();
        }
    };

    public static MutableBlockVector2D get(int x, int z) {
        return MUTABLE_CACHE.get().setComponents(x, z);
    }

    private transient int x, z;

    public MutableBlockVector2D() {
        this(0, 0);
    }
    public static final MutableBlockVector2D ZERO = new MutableBlockVector2D(0, 0);
    public static final MutableBlockVector2D UNIT_X = new MutableBlockVector2D(1, 0);
    public static final MutableBlockVector2D UNIT_Z = new MutableBlockVector2D(0, 1);
    public static final MutableBlockVector2D ONE = new MutableBlockVector2D(1, 1);

    /**
     * A comparator for MutableBlockVector2Ds that orders the vectors by rows, with x as the
     * column and z as the row.
     *
     * For example, if x is the horizontal axis and z is the vertical axis, it
     * sorts like so:
     *
     * <pre>
     * 0123
     * 4567
     * 90ab
     * cdef
     * </pre>
     */
    public static final Comparator<MutableBlockVector2D> COMPARING_GRID_ARRANGEMENT = (a, b) -> {
        return ComparisonChain.start()
                .compare(a.getBlockZ(), b.getBlockZ())
                .compare(a.getBlockX(), b.getBlockX())
                .result();
    };
    
    /**
     * Construct an instance.
     * 
     * @param vector
     */
    public MutableBlockVector2D(BlockVector2 vector) {
    	this(vector.getBlockX(), vector.getBlockZ());
    }

    /**
     * Construct an instance.
     *
     * @param x the X coordinate
     * @param z the Z coordinate
     */
    public MutableBlockVector2D(double x, double z) {
        this((int) Math.floor(x), (int) Math.floor(z));
    }

    /**
     * Construct an instance.
     *
     * @param x the X coordinate
     * @param z the Z coordinate
     */
    public MutableBlockVector2D(int x, int z) {
        this.x = x;
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
    public MutableBlockVector2D add(MutableBlockVector2D other) {
        return add(other.x, other.z);
    }

    /**
     * Add another vector to this vector.
     *
     * @param x the value to add
     * @param z the value to add
     * @return a new vector
     */
    public MutableBlockVector2D add(int x, int z) {
    	this.x += x;
    	this.z += z;
        return this;
    }

    /**
     * Add a list of vectors to this vector.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public MutableBlockVector2D add(MutableBlockVector2D... others) {

        for (MutableBlockVector2D other : others) {
            x += other.x;
            x += other.z;
        }

        return this;
    }

    /**
     * Subtract another vector from this vector.
     *
     * @param other the other vector
     * @return a new vector
     */
    public MutableBlockVector2D subtract(MutableBlockVector2D other) {
        return subtract(other.x, other.z);
    }

    /**
     * Subtract another vector from this vector.
     *
     * @param x the value to subtract
     * @param z the value to subtract
     * @return a new vector
     */
    public MutableBlockVector2D subtract(int x, int z) {
    	this.x -= x;
    	this.z -= z;
        return this;
    }

    /**
     * Subtract a list of vectors from this vector.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public MutableBlockVector2D subtract(MutableBlockVector2D... others) {

        for (MutableBlockVector2D other : others) {
            x -= other.x;
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
    public MutableBlockVector2D multiply(MutableBlockVector2D other) {
        return multiply(other.x, other.z);
    }

    /**
     * Multiply this vector by another vector on each component.
     *
     * @param x the value to multiply
     * @param z the value to multiply
     * @return a new vector
     */
    public MutableBlockVector2D multiply(int x, int z) {
    	this.x *= x;
    	this.z *= z;
        return this;
    }

    /**
     * Multiply this vector by zero or more vectors on each component.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public MutableBlockVector2D multiply(MutableBlockVector2D... others) {

        for (MutableBlockVector2D other : others) {
            x *= other.x;
            z *= other.z;
        }

        return this;
    }

    /**
     * Perform scalar multiplication.
     *
     * @param n the value to multiply
     * @return a new vector
     */
    public MutableBlockVector2D multiply(int n) {
        return multiply(n, n);
    }

    /**
     * Divide this vector by another vector on each component.
     *
     * @param other the other vector
     * @return a new vector
     */
    public MutableBlockVector2D divide(MutableBlockVector2D other) {
        return divide(other.x, other.z);
    }

    /**
     * Divide this vector by another vector on each component.
     *
     * @param x the value to divide by
     * @param z the value to divide by
     * @return a new vector
     */
    public MutableBlockVector2D divide(int x, int z) {
    	this.x /= x;
    	this.z /= z;
        return this;
    }

    /**
     * Perform scalar division.
     *
     * @param n the value to divide by
     * @return a new vector
     */
    public MutableBlockVector2D divide(int n) {
        return divide(n, n);
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
        return x * x + z * z;
    }

    /**
     * Get the distance between this vector and another vector.
     *
     * @param other the other vector
     * @return distance
     */
    public double distance(MutableBlockVector2D other) {
        return Math.sqrt(distanceSq(other));
    }

    /**
     * Get the distance between this vector and another vector, squared.
     *
     * @param other the other vector
     * @return distance
     */
    public int distanceSq(MutableBlockVector2D other) {
        int dx = other.x - x;
        int dz = other.z - z;
        return dx * dx + dz * dz;
    }

    /**
     * Get the normalized vector, which is the vector divided by its
     * length.
     *
     * @return a new vector
     */
    public MutableBlockVector2D normalize() {
        double len = length();
        this.x /= len;
        this.z /= len;
        return this;
    }

    /**
     * Gets the dot product of this and another vector.
     *
     * @param other the other vector
     * @return the dot product of this and the other vector
     */
    public int dot(MutableBlockVector2D other) {
        return x * other.x + z * other.z;
    }

    /**
     * Checks to see if a vector is contained with another.
     *
     * @param min the minimum point (X, Y, and Z are the lowest)
     * @param max the maximum point (X, Y, and Z are the lowest)
     * @return true if the vector is contained
     */
    public boolean containedWithin(MutableBlockVector2D min, MutableBlockVector2D max) {
        return x >= min.x && x <= max.x
                && z >= min.z && z <= max.z;
    }

    /**
     * Floors the values of all components.
     *
     * @return a new vector
     */
    public MutableBlockVector2D floor() {
        // already floored, kept for feature parity with Vector2
        return this;
    }

    /**
     * Rounds all components up.
     *
     * @return a new vector
     */
    public MutableBlockVector2D ceil() {
        // already raised, kept for feature parity with Vector2
        return this;
    }

    /**
     * Rounds all components to the closest integer.
     *
     * <p>Components &lt; 0.5 are rounded down, otherwise up.</p>
     *
     * @return a new vector
     */
    public MutableBlockVector2D round() {
        // already rounded, kept for feature parity with Vector2
        return this;
    }

    /**
     * Returns a vector with the absolute values of the components of
     * this vector.
     *
     * @return a new vector
     */
    public MutableBlockVector2D abs() {
    	x = Math.abs(x);
    	z = Math.abs(z);
        return this;
    }

    /**
     * Perform a 2D transformation on this vector.
     *
     * @param angle in degrees
     * @param aboutX about which x coordinate to rotate
     * @param aboutZ about which z coordinate to rotate
     * @param translateX what to add after rotation
     * @param translateZ what to add after rotation
     * @return a new vector
     * @see AffineTransform another method to transform vectors
     */
    public MutableBlockVector2D transform2D(double angle, double aboutX, double aboutZ, double translateX, double translateZ) {
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
     * Gets the minimum components of two vectors.
     *
     * @param v2 the second vector
     * @return minimum
     */
    public MutableBlockVector2D getMinimum(MutableBlockVector2D v2) {
    	x = Math.min(x, v2.x);
    	z = Math.min(z,  v2.z);
        return this;
    }

    /**
     * Gets the maximum components of two vectors.
     *
     * @param v2 the second vector
     * @return maximum
     */
    public MutableBlockVector2D getMaximum(MutableBlockVector2D v2) {
    	x = Math.max(x, v2.x);
    	z = Math.max(z,  v2.z);
        return this;
    }
    
    /**
     * Creates a 2D vector from this vector.
     *
     * @return a new vector
     */
    public Vector2 toVector2() {
        return new Vector2(x, z);
    }

    /**
     * Creates a 3D vector by adding a zero Y component to this vector.
     *
     * @return a new vector
     */
    public Vector3 toVector3() {
        return toVector3(0);
    }

    /**
     * Creates a 3D vector by adding the specified Y component to this vector.
     *
     * @param y the Y component
     * @return a new vector
     */
    public Vector3 toVector3(double y) {
        return new Vector3(x, y, z);
    }

    /**
     * Creates a 3D vector by adding a zero Y component to this vector.
     *
     * @return a new vector
     */
    public BlockVector3 toBlockVector3() {
        return toBlockVector3(0);
    }

    /**
     * Creates a 3D vector by adding the specified Y component to this vector.
     *
     * @param y the Y component
     * @return a new vector
     */
    public BlockVector3 toBlockVector3(int y) {
        return new BlockVector3(x, y, z);
    }
    
    /**
     * Creates a 2D vector from this vector.
     *
     * @return a new vector
     */
    public BlockVector2 toBlockVector2() {
    	return new BlockVector2(x, z);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MutableBlockVector2D)) {
            return false;
        }

        MutableBlockVector2D other = (MutableBlockVector2D) obj;
        return other.x == this.x && other.z == this.z;

    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + Integer.hashCode(x);
        hash = 31 * hash + Integer.hashCode(z);
        return hash;
    }

    @Override
    public String toString() {
        return "Mutable (" + x + ", " + z + ")";
    }

    public MutableBlockVector2D setComponents(int x, int z) {
        this.x = x;
        this.z = z;
        return this;
    }

    public MutableBlockVector2D setComponents(double x, double z) {
        return setComponents((int) x, (int) z);
    }

    public final void mutX(int x) {
        this.x = x;
    }

    public void mutZ(int z) {
        this.z = z;
    }

    public final void mutX(double x) {
        this.x = (int) x;
    }

    public void mutZ(double z) {
        this.z = (int) z;
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.writeInt(x);
        stream.writeInt(z);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        this.x = stream.readInt();
        this.z = stream.readInt();
    }

    public MutableBlockVector2D nextPosition() {
        int absX = Math.abs(x);
        int absY = Math.abs(z);
        if (absX > absY) {
            if (x > 0) {
                return setComponents(x, z + 1);
            } else {
                return setComponents(x, z - 1);
            }
        } else if (absY > absX) {
            if (z > 0) {
                return setComponents(x - 1, z);
            } else {
                return setComponents(x + 1, z);
            }
        } else {
            if (x == z && x > 0) {
                return setComponents(x, z + 1);
            }
            if (x == absX) {
                return setComponents(x, z + 1);
            }
            if (z == absY) {
                return setComponents(x, z - 1);
            }
            return setComponents(x + 1, z);
        }
    }
}