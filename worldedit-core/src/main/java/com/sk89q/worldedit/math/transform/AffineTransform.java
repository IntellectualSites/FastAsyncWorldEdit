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

package com.sk89q.worldedit.math.transform;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MathUtils;
import com.sk89q.worldedit.math.Vector3;

import java.io.Serializable;

/**
 * An affine transform.
 *
 * <p>This class is from the
 * <a href="http://geom-java.sourceforge.net/index.html">JavaGeom project</a>,
 * which is licensed under LGPL v2.1.</p>
 */
//FAWE start - made Serializable
public class AffineTransform implements Transform, Serializable {
//FAWE end

    /**
     * coefficients for x coordinate.
     */
    private final double m00; // x-only
    private final double m01; // x-y
    private final double m02; // x-z
    private final double m03; // translation

    /**
     * coefficients for y coordinate.
     */
    private final double m10; // x-y
    private final double m11; // y-only
    private final double m12; // y-z
    private final double m13; // translation

    /**
     * coefficients for z coordinate.
     */
    private final double m20; // x-z
    private final double m21; // y-z
    private final double m22; // z-only
    private final double m23; // translation

    // ===================================================================
    // constructors

    /**
     * Creates a new affine transform3D set to the identity.
     */
    public AffineTransform() {
        // init to identity matrix
        m00 = m11 = m22 = 1;
        m01 = m02 = m03 = 0;
        m10 = m12 = m13 = 0;
        m20 = m21 = m23 = 0;
    }

    public AffineTransform(double[] coefs) {
        if (coefs.length == 9) {
            m00 = coefs[0];
            m01 = coefs[1];
            m02 = coefs[2];
            m10 = coefs[3];
            m11 = coefs[4];
            m12 = coefs[5];
            m20 = coefs[6];
            m21 = coefs[7];
            m22 = coefs[8];
            m03 = m13 = m23 = 0;
        } else if (coefs.length == 12) {
            m00 = coefs[0];
            m01 = coefs[1];
            m02 = coefs[2];
            m03 = coefs[3];
            m10 = coefs[4];
            m11 = coefs[5];
            m12 = coefs[6];
            m13 = coefs[7];
            m20 = coefs[8];
            m21 = coefs[9];
            m22 = coefs[10];
            m23 = coefs[11];
        } else {
            throw new IllegalArgumentException(
                    "Input array must have 9 or 12 elements");
        }
    }

    public AffineTransform(
            double xx, double yx, double zx, double tx,
            double xy, double yy, double zy, double ty, double xz, double yz,
            double zz, double tz
    ) {
        m00 = xx;
        m01 = yx;
        m02 = zx;
        m03 = tx;
        m10 = xy;
        m11 = yy;
        m12 = zy;
        m13 = ty;
        m20 = xz;
        m21 = yz;
        m22 = zz;
        m23 = tz;
    }

    // ===================================================================
    // accessors

    @Override
    public boolean isIdentity() {
        return m00 == m11 && m11 == m22 && m22 == 1
                && m01 == m02 && m02 == m03 && m03 == 0
                && m10 == m12 && m12 == m13 && m13 == 0
                && m20 == m21 && m21 == m23 && m23 == 0;
    }

    /**
     * Returns the affine coefficients of the transform. Result is an array of
     * 12 double.
     */
    public double[] coefficients() {
        return new double[]{m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23};
    }

    //FAWE start
    public boolean isOffAxis() {
        double[] c = coefficients();
        for (int i = 0; i < c.length; i++) {
            if ((i + 1) % 4 != 0) {
                if (Math.abs(c[i]) != 1 && c[i] != 0) {
                    return true;
                }
            }
        }
        return false;
    }
    //FAWE end

    /**
     * Computes the determinant of this transform. Can be zero.
     *
     * @return the determinant of the transform.
     */
    private double determinant() {
        return m00 * (m11 * m22 - m12 * m21) - m01 * (m10 * m22 - m20 * m12)
                + m02 * (m10 * m21 - m20 * m11);
    }

    /**
     * Computes the inverse affine transform.
     */
    @Override
    public AffineTransform inverse() {
        double det = this.determinant();
        return new AffineTransform(
                (m11 * m22 - m21 * m12) / det,
                (m21 * m02 - m01 * m22) / det,
                (m01 * m12 - m11 * m02) / det,
                (m01 * (m22 * m13 - m12 * m23) + m02 * (m11 * m23 - m21 * m13)
                        - m03 * (m11 * m22 - m21 * m12)) / det,
                (m20 * m12 - m10 * m22) / det,
                (m00 * m22 - m20 * m02) / det,
                (m10 * m02 - m00 * m12) / det,
                (m00 * (m12 * m23 - m22 * m13) - m02 * (m10 * m23 - m20 * m13)
                        + m03 * (m10 * m22 - m20 * m12)) / det,
                (m10 * m21 - m20 * m11) / det,
                (m20 * m01 - m00 * m21) / det,
                (m00 * m11 - m10 * m01) / det,
                (m00 * (m21 * m13 - m11 * m23) + m01 * (m10 * m23 - m20 * m13)
                        - m03 * (m10 * m21 - m20 * m11)) / det
        );
    }

    // ===================================================================
    // general methods

    /**
     * Returns the affine transform created by applying first the affine
     * transform given by the parameters, then this affine transform.
     *
     * @return the composition this * that
     */
    public AffineTransform concatenate(double o00, double o01, double o02, double o03,
                                       double o10, double o11, double o12, double o13,
                                       double o20, double o21, double o22, double o23) {
        double n00 = m00 * o00 + m01 * o10 + m02 * o20;
        double n01 = m00 * o01 + m01 * o11 + m02 * o21;
        double n02 = m00 * o02 + m01 * o12 + m02 * o22;
        double n03 = m00 * o03 + m01 * o13 + m02 * o23 + m03;
        double n10 = m10 * o00 + m11 * o10 + m12 * o20;
        double n11 = m10 * o01 + m11 * o11 + m12 * o21;
        double n12 = m10 * o02 + m11 * o12 + m12 * o22;
        double n13 = m10 * o03 + m11 * o13 + m12 * o23 + m13;
        double n20 = m20 * o00 + m21 * o10 + m22 * o20;
        double n21 = m20 * o01 + m21 * o11 + m22 * o21;
        double n22 = m20 * o02 + m21 * o12 + m22 * o22;
        double n23 = m20 * o03 + m21 * o13 + m22 * o23 + m23;
        return new AffineTransform(
                n00, n01, n02, n03,
                n10, n11, n12, n13,
                n20, n21, n22, n23);
    }

    /**
     * Returns the affine transform created by applying first the affine
     * transform given by {@code that}, then this affine transform.
     *
     * @param that the transform to apply first
     * @return the composition this * that
     */
    public AffineTransform concatenate(AffineTransform that) {
        return concatenate(
                that.m00, that.m01, that.m02, that.m03,
                that.m10, that.m11, that.m12, that.m13,
                that.m20, that.m21, that.m22, that.m23
        );
    }

    /**
     * Return the affine transform created by applying first this affine
     * transform, then the affine transform given by {@code that}.
     *
     * @param that the transform to apply in a second step
     * @return the composition that * this
     */
    public AffineTransform preConcatenate(AffineTransform that) {
        double n00 = that.m00 * m00 + that.m01 * m10 + that.m02 * m20;
        double n01 = that.m00 * m01 + that.m01 * m11 + that.m02 * m21;
        double n02 = that.m00 * m02 + that.m01 * m12 + that.m02 * m22;
        double n03 = that.m00 * m03 + that.m01 * m13 + that.m02 * m23 + that.m03;
        double n10 = that.m10 * m00 + that.m11 * m10 + that.m12 * m20;
        double n11 = that.m10 * m01 + that.m11 * m11 + that.m12 * m21;
        double n12 = that.m10 * m02 + that.m11 * m12 + that.m12 * m22;
        double n13 = that.m10 * m03 + that.m11 * m13 + that.m12 * m23 + that.m13;
        double n20 = that.m20 * m00 + that.m21 * m10 + that.m22 * m20;
        double n21 = that.m20 * m01 + that.m21 * m11 + that.m22 * m21;
        double n22 = that.m20 * m02 + that.m21 * m12 + that.m22 * m22;
        double n23 = that.m20 * m03 + that.m21 * m13 + that.m22 * m23 + that.m23;
        return new AffineTransform(
                n00, n01, n02, n03,
                n10, n11, n12, n13,
                n20, n21, n22, n23
        );
    }

    public AffineTransform translate(Vector3 vec) {
        return translate(vec.getX(), vec.getY(), vec.getZ());
    }

    public AffineTransform translate(BlockVector3 vec) {
        return translate(vec.getX(), vec.getY(), vec.getZ());
    }

    public AffineTransform translate(double x, double y, double z) {
        return concatenate(1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z);
    }

    public AffineTransform rotateX(double theta) {
        double cot = MathUtils.dCos(theta);
        double sit = MathUtils.dSin(theta);
        return concatenate(
                1, 0, 0, 0,
                0, cot, -sit, 0,
                0, sit, cot, 0
        );
    }

    public AffineTransform rotateY(double theta) {
        double cot = MathUtils.dCos(theta);
        double sit = MathUtils.dSin(theta);
        return concatenate(
                cot, 0, sit, 0,
                0, 1, 0, 0,
                -sit, 0, cot, 0
        );
    }

    public AffineTransform rotateZ(double theta) {
        double cot = MathUtils.dCos(theta);
        double sit = MathUtils.dSin(theta);
        return concatenate(
                cot, -sit, 0, 0,
                sit, cot, 0, 0,
                0, 0, 1, 0
        );
    }

    public AffineTransform scale(double s) {
        return scale(s, s, s);
    }

    public AffineTransform scale(double sx, double sy, double sz) {
        return concatenate(sx, 0, 0, 0, 0, sy, 0, 0, 0, 0, sz, 0);
    }

    public AffineTransform scale(Vector3 vec) {
        return scale(vec.getX(), vec.getY(), vec.getZ());
    }

    //FAWE start
    public boolean isScaled(Vector3 vector) {
        boolean flip = vector.getX() != 0 && m00 < 0;
        if (vector.getY() != 0 && m11 < 0) {
            flip = !flip;
        }
        if (vector.getZ() != 0 && m22 < 0) {
            flip = !flip;
        }
        if (flip) {
            return true;
        }
        // Check for flip-and-rotate
        if (vector.getX() != 0 && vector.getY() != 0 && ((m01 < 0 && m10 < 0) || (m01 > 0 && m10 > 0))) {
            flip = true;
        }
        if (vector.getX() != 0 && vector.getZ() != 0 && ((m02 < 0 && m20 < 0) || (m02 > 0 && m20 > 0))) {
            flip = !flip;
        }
        if (vector.getY() != 0 && vector.getZ() != 0 && ((m12 < 0 && m21 < 0) || (m12 > 0 && m21 > 0))) {
            flip = !flip;
        }
        return flip;
    }
    //FAWE end

    @Override
    public Vector3 apply(Vector3 vector) {
        double x = (vector.getX() * m00 + vector.getY() * m01 + vector.getZ() * m02 + m03);
        double y = (vector.getX() * m10 + vector.getY() * m11 + vector.getZ() * m12 + m13);
        double z = (vector.getX() * m20 + vector.getY() * m21 + vector.getZ() * m22 + m23);
        vector = vector.mutX(x);
        vector = vector.mutY(y);
        vector = vector.mutZ(z);
        return vector;
    }

    public AffineTransform combine(AffineTransform other) {
        return concatenate(other);
    }

    @Override
    public Transform combine(Transform other) {
        //FAWE start - check other identity
        if (other instanceof Identity || other.isIdentity()) {
            return this;
        } else if (other instanceof AffineTransform otherTransform) {
            //FAWE end
            return concatenate(otherTransform);
        } else {
            return new CombinedTransform(this, other);
        }
    }

    /**
     * Returns if this affine transform represents a horizontal flip.
     */
    public boolean isHorizontalFlip() {
        // use the determinant of the x-z submatrix to check if this is a horizontal flip
        return m00 * m22 - m02 * m20 < 0;
    }

    /**
     * Returns if this affine transform represents a vertical flip.
     */
    public boolean isVerticalFlip() {
        return m11 < 0;
    }

    @Override
    public String toString() {
        return String.format(
                "Affine[%g %g %g %g, %g %g %g %g, %g %g %g %g]}",
                m00,
                m01,
                m02,
                m03,
                m10,
                m11,
                m12,
                m13,
                m20,
                m21,
                m22,
                m23
        );
    }


}
