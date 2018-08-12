package com.boydti.fawe.util;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.IllegalPathStateException;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Vector;

/**
 * <a href="https://github.com/Sciss/ShapeInterpolator/blob/master/src/main/java/de/sciss/shapeint/ShapeInterpolator.java">Original source</a><br>
 * An interpolator for {@link Shape} objects.
 * This class can be used to morph between the geometries
 * of two relatively arbitrary shapes with the only restrictions being
 * that the two different numbers of sub-paths or two shapes with
 * disparate winding rules may not blend together in a pleasing
 * manner.
 * The ShapeEvaluator will do the best job it can if the shapes do
 * not match in winding rule or number of sub-paths, but the geometry
 * of the shapes may need to be adjusted by other means to make the
 * shapes more like each other for best aesthetic effect.
 * <p>
 * Note that the process of comparing two geometries and finding similar
 * structures between them to blend for the morphing operation can be
 * expensive.
 * Instances of this class will properly perform the necessary
 * geometric analysis of their arguments on every method call and attempt
 * to cache the information so that they can operate more quickly if called
 * multiple times in a row on the same pair of {@code Shape} objects.
 * As a result attempting to mutate a {@code Shape} object that is stored
 * in one of their keyframes may not have any effect if the associated
 * interpolator has already cached the geometry.
 * Also, it is advisable to use different instances of {@code ShapeEvaluator}
 * for every pair of keyframes being morphed so that the cached information
 * can be reused as much as possible.
 */
public class ShapeInterpolator {

    private Shape savedV0;
    private Shape savedV1;
    private Geometry geom0;
    private Geometry geom1;

    public static Shape apply(Shape v0, Shape v1, float fraction) {
        return apply(v0, v1, fraction, false);
    }

    public static Shape apply(Shape v0, Shape v1, float fraction, boolean unionBounds) {
        final ShapeInterpolator instance = new ShapeInterpolator();
        return instance.evaluate(v0, v1, fraction, unionBounds);
    }

    /**
     * Creates an interpolated shape from tight bounds.
     */
    public Shape evaluate(Shape v0, Shape v1, float fraction) {
        return evaluate(v0, v1, fraction, false);
    }

    /**
     * Creates an interpolated shape.
     *
     * @param v0          the first shape
     * @param v1          the second shape
     * @param fraction    the fraction from zero (just first shape) to one (just second shape)
     * @param unionBounds if `true`, the shape reports bounds which are the union of
     *                    the bounds of both shapes, if `false` it reports "tight" bounds
     *                    using the actual interpolated path.
     */
    public Shape evaluate(Shape v0, Shape v1, float fraction, boolean unionBounds) {
        if (savedV0 != v0 || savedV1 != v1) {
            if (savedV0 == v1 && savedV1 == v0) {
                // Just swap the geometries
                final Geometry tmp = geom0;
                geom0 = geom1;
                geom1 = tmp;
            } else {
                recalculate(v0, v1);
            }
            savedV0 = v0;
            savedV1 = v1;
        }
        return getShape(fraction, unionBounds);
    }

    private void recalculate(Shape v0, Shape v1) {
        geom0 = new Geometry(v0);
        geom1 = new Geometry(v1);
        final float[] tVals0 = geom0.getTVals();
        final float[] tVals1 = geom1.getTVals();
        final float[] masterTVals = mergeTVals(tVals0, tVals1);
        geom0.setTVals(masterTVals);
        geom1.setTVals(masterTVals);
    }

    private Shape getShape(float fraction, boolean unionBounds) {
        return new MorphedShape(geom0, geom1, fraction, unionBounds);
    }

    private static float[] mergeTVals(float[] tVals0, float[] tVals1) {
        final int count = sortTVals(tVals0, tVals1, null);
        final float[] newTVals = new float[count];
        sortTVals(tVals0, tVals1, newTVals);
        return newTVals;
    }

    private static int sortTVals(float[] tVals0,
                                 float[] tVals1,
                                 float[] newTVals) {
        int i0 = 0;
        int i1 = 0;
        int numTVals = 0;
        while (i0 < tVals0.length && i1 < tVals1.length) {
            final float t0 = tVals0[i0];
            final float t1 = tVals1[i1];
            if (t0 <= t1) {
                if (newTVals != null) {
                    newTVals[numTVals] = t0;
                }
                i0++;
            }
            if (t1 <= t0) {
                if (newTVals != null) {
                    newTVals[numTVals] = t1;
                }
                i1++;
            }
            numTVals++;
        }
        return numTVals;
    }

    private static float interp(float v0, float v1, float t) {
        return (v0 + ((v1 - v0) * t));
    }

    private static class Geometry {
        static final float THIRD = (1f / 3f);
        static final float MIN_LEN = 0.001f;

        final int windingRule;
        float[] bezierCoordinates;
        int numCoordinates;
        float[] myTVals;

        public Geometry(Shape s) {
            // Multiple of 6 plus 2 more for initial move-to
            bezierCoordinates = new float[20];
            final PathIterator pi = s.getPathIterator(null);
            windingRule = pi.getWindingRule();
            if (pi.isDone()) {
                // We will have 1 segment and it will be all zeros
                // It will have 8 coordinates (2 for move-to, 6 for cubic)
                numCoordinates = 8;
            }
            final float[] coordinates = new float[6];
            int type = pi.currentSegment(coordinates);
            pi.next();
            if (type != PathIterator.SEG_MOVETO) {
                throw new IllegalPathStateException("missing initial move-to");
            }
            float curX, curY, movX, movY;
            bezierCoordinates[0] = curX = movX = coordinates[0];
            bezierCoordinates[1] = curY = movY = coordinates[1];
            float newX, newY;
            final Vector<Point2D.Float> savedPathEndPoints = new Vector<Point2D.Float>();
            numCoordinates = 2;
            while (!pi.isDone()) {
                switch (pi.currentSegment(coordinates)) {
                    case PathIterator.SEG_MOVETO:
                        if (curX != movX || curY != movY) {
                            appendLineTo(curX, curY, movX, movY);
                            curX = movX;
                            curY = movY;
                        }
                        newX = coordinates[0];
                        newY = coordinates[1];
                        if (curX != newX || curY != newY) {
                            savedPathEndPoints.add(new Point2D.Float(movX, movY));
                            appendLineTo(curX, curY, newX, newY);
                            curX = movX = newX;
                            curY = movY = newY;
                        }
                        break;
                    case PathIterator.SEG_CLOSE:
                        if (curX != movX || curY != movY) {
                            appendLineTo(curX, curY, movX, movY);
                            curX = movX;
                            curY = movY;
                        }
                        break;
                    case PathIterator.SEG_LINETO:
                        newX = coordinates[0];
                        newY = coordinates[1];
                        appendLineTo(curX, curY, newX, newY);
                        curX = newX;
                        curY = newY;
                        break;
                    case PathIterator.SEG_QUADTO:
                        final float ctrlX = coordinates[0];
                        final float ctrlY = coordinates[1];
                        newX = coordinates[2];
                        newY = coordinates[3];
                        appendQuadTo(curX, curY, ctrlX, ctrlY, newX, newY);
                        curX = newX;
                        curY = newY;
                        break;
                    case PathIterator.SEG_CUBICTO:
                        newX = coordinates[4];
                        newY = coordinates[5];
                        appendCubicTo(
                                coordinates[0], coordinates[1],
                                coordinates[2], coordinates[3],
                                newX, newY);
                        curX = newX;
                        curY = newY;
                        break;
                }
                pi.next();
            }
            // Add closing segment if either:
            // - we only have initial move-to - expand it to an empty cubic
            // - or we are not back to the starting point
            if ((numCoordinates < 8) || curX != movX || curY != movY) {
                appendLineTo(curX, curY, movX, movY);
                curX = movX;
                curY = movY;
            }
            // Now retrace our way back through all of the connecting
            // inter-sub-path segments
            for (int i = savedPathEndPoints.size() - 1; i >= 0; i--) {
                final Point2D.Float p = savedPathEndPoints.get(i);
                newX = p.x;
                newY = p.y;
                if (curX != newX || curY != newY) {
                    appendLineTo(curX, curY, newX, newY);
                    curX = newX;
                    curY = newY;
                }
            }
            // Now find the segment endpoint with the smallest Y coordinate
            int minPt = 0;
            float minX = bezierCoordinates[0];
            float minY = bezierCoordinates[1];
            for (int ci = 6; ci < numCoordinates; ci += 6) {
                float x = bezierCoordinates[ci];
                float y = bezierCoordinates[ci + 1];
                if (y < minY || (y == minY && x < minX)) {
                    minPt = ci;
                    minX = x;
                    minY = y;
                }
            }
            // If the smallest Y coordinate is not the first coordinate,
            // rotate the points so that it is...
            if (minPt > 0) {
                // Keep in mind that first 2 coordinates == last 2 coordinates
                final float[] newCoordinates = new float[numCoordinates];
                // Copy all coordinates from minPt to the end of the
                // array to the beginning of the new array
                System.arraycopy(bezierCoordinates, minPt,
                        newCoordinates, 0,
                        numCoordinates - minPt);
                // Now we do not want to copy 0,1 as they are duplicates
                // of the last 2 coordinates which we just copied.  So
                // we start the source copy at index 2, but we still
                // copy a full minPt coordinates which copies the two
                // coordinates that were at minPt to the last two elements
                // of the array, thus ensuring that thew new array starts
                // and ends with the same pair of coordinates...
                System.arraycopy(bezierCoordinates, 2,
                        newCoordinates, numCoordinates - minPt,
                        minPt);
                bezierCoordinates = newCoordinates;
            }
            /* Clockwise enforcement:
             * - This technique is based on the formula for calculating
             *   the area of a Polygon.  The standard formula is:
             *   Area(Poly) = 1/2 * sum(x[i]*y[i+1] - x[i+1]y[i])
             * - The returned area is negative if the polygon is
             *   "mostly clockwise" and positive if the polygon is
             *   "mostly counter-clockwise".
             * - One failure mode of the Area calculation is if the
             *   Polygon is self-intersecting.  This is due to the
             *   fact that the areas on each side of the self-intersection
             *   are bounded by segments which have opposite winding
             *   direction.  Thus, those areas will have opposite signs
             *   on the accumulation of their area summations and end
             *   up canceling each other out partially.
             * - This failure mode of the algorithm in determining the
             *   exact magnitude of the area is not actually a big problem
             *   for our needs here since we are only using the sign of
             *   the resulting area to figure out the overall winding
             *   direction of the path.  If self-intersections cause
             *   different parts of the path to disagree as to the
             *   local winding direction, that is no matter as we just
             *   wait for the final answer to tell us which winding
             *   direction had greater representation.  If the final
             *   result is zero then the path was equal parts clockwise
             *   and counter-clockwise and we do not care about which
             *   way we order it as either way will require half of the
             *   path to unwind and re-wind itself.
             */
            float area = 0;
            // Note that first and last points are the same so we
            // do not need to process coordinates[0,1] against coordinates[n-2,n-1]
            curX = bezierCoordinates[0];
            curY = bezierCoordinates[1];
            for (int i = 2; i < numCoordinates; i += 2) {
                newX = bezierCoordinates[i];
                newY = bezierCoordinates[i + 1];
                area += curX * newY - newX * curY;
                curX = newX;
                curY = newY;
            }
            if (area < 0) {
                /* The area is negative so the shape was clockwise
                 * in a Euclidean sense.  But, our screen coordinate
                 * systems have the origin in the upper left so they
                 * are flipped.  Thus, this path "looks" ccw on the
                 * screen so we are flipping it to "look" clockwise.
                 * Note that the first and last points are the same
                 * so we do not need to swap them.
                 * (Not that it matters whether the paths end up cw
                 *  or ccw in the end as long as all of them are the
                 *  same, but above we called this section "Clockwise
                 *  Enforcement", so we do not want to be liars. ;-)
                 */
                // Note that [0,1] do not need to be swapped with [n-2,n-1]
                // So first pair to swap is [2,3] and [n-4,n-3]
                int i = 2;
                int j = numCoordinates - 4;
                while (i < j) {
                    curX = bezierCoordinates[i];
                    curY = bezierCoordinates[i + 1];
                    bezierCoordinates[i] = bezierCoordinates[j];
                    bezierCoordinates[i + 1] = bezierCoordinates[j + 1];
                    bezierCoordinates[j] = curX;
                    bezierCoordinates[j + 1] = curY;
                    i += 2;
                    j -= 2;
                }
            }
        }

        private void appendLineTo(float x0, float y0,
                                  float x1, float y1) {
            appendCubicTo(// A third of the way from xy0 to xy1:
                    interp(x0, x1, THIRD),
                    interp(y0, y1, THIRD),
                    // A third of the way from xy1 back to xy0:
                    interp(x1, x0, THIRD),
                    interp(y1, y0, THIRD),
                    x1, y1);
        }

        private void appendQuadTo(float x0, float y0,
                                  float ctrlX, float ctrlY,
                                  float x1, float y1) {
            appendCubicTo(// A third of the way from ctrl X/Y back to xy0:
                    interp(ctrlX, x0, THIRD),
                    interp(ctrlY, y0, THIRD),
                    // A third of the way from ctrl X/Y to xy1:
                    interp(ctrlX, x1, THIRD),
                    interp(ctrlY, y1, THIRD),
                    x1, y1);
        }

        private void appendCubicTo(float ctrlX1, float ctrlY1,
                                   float ctrlX2, float ctrlY2,
                                   float x1, float y1) {
            if (numCoordinates + 6 > bezierCoordinates.length) {
                // Keep array size to a multiple of 6 plus 2
                int newsize = (numCoordinates - 2) * 2 + 2;
                final float[] newCoordinates = new float[newsize];
                System.arraycopy(bezierCoordinates, 0, newCoordinates, 0, numCoordinates);
                bezierCoordinates = newCoordinates;
            }
            bezierCoordinates[numCoordinates++] = ctrlX1;
            bezierCoordinates[numCoordinates++] = ctrlY1;
            bezierCoordinates[numCoordinates++] = ctrlX2;
            bezierCoordinates[numCoordinates++] = ctrlY2;
            bezierCoordinates[numCoordinates++] = x1;
            bezierCoordinates[numCoordinates++] = y1;
        }

        public int getWindingRule() {
            return windingRule;
        }

        public int getNumCoordinates() {
            return numCoordinates;
        }

        public float getCoordinate(int i) {
            return bezierCoordinates[i];
        }

        public float[] getTVals() {
            if (myTVals != null) {
                return myTVals;
            }

            // assert(numCoordinates >= 8);
            // assert(((numCoordinates - 2) % 6) == 0);
            final float[] tVals = new float[(numCoordinates - 2) / 6 + 1];

            // First calculate total "length" of path
            // Length of each segment is averaged between
            // the length between the endpoints (a lower bound for a cubic)
            // and the length of the control polygon (an upper bound)
            float segX = bezierCoordinates[0];
            float segY = bezierCoordinates[1];
            float tLen = 0;
            int ci = 2;
            int ti = 0;
            while (ci < numCoordinates) {
                float prevX, prevY, newX, newY;
                prevX = segX;
                prevY = segY;
                newX = bezierCoordinates[ci++];
                newY = bezierCoordinates[ci++];
                prevX -= newX;
                prevY -= newY;
                float len = (float) Math.sqrt(prevX * prevX + prevY * prevY);
                prevX = newX;
                prevY = newY;
                newX = bezierCoordinates[ci++];
                newY = bezierCoordinates[ci++];
                prevX -= newX;
                prevY -= newY;
                len += (float) Math.sqrt(prevX * prevX + prevY * prevY);
                prevX = newX;
                prevY = newY;
                newX = bezierCoordinates[ci++];
                newY = bezierCoordinates[ci++];
                prevX -= newX;
                prevY -= newY;
                len += (float) Math.sqrt(prevX * prevX + prevY * prevY);
                // len is now the total length of the control polygon
                segX -= newX;
                segY -= newY;
                len += (float) Math.sqrt(segX * segX + segY * segY);
                // len is now sum of linear length and control polygon length
                len /= 2;
                // len is now average of the two lengths

                /* If the result is zero length then we will have problems
                 * below trying to do the math and bookkeeping to split
                 * the segment or pair it against the segments in the
                 * other shape.  Since these lengths are just estimates
                 * to map the segments of the two shapes onto corresponding
                 * segments of "approximately the same length", we will
                 * simply modify the length of this segment to be at least
                 * a minimum value and it will simply grow from zero or
                 * near zero length to a non-trivial size as it morphs.
                 */
                if (len < MIN_LEN) {
                    len = MIN_LEN;
                }
                tLen += len;
                tVals[ti++] = tLen;
                segX = newX;
                segY = newY;
            }

            // Now set tVals for each segment to its proportional
            // part of the length
            float prevT = tVals[0];
            tVals[0] = 0;
            for (ti = 1; ti < tVals.length - 1; ti++) {
                final float nextT = tVals[ti];
                tVals[ti] = prevT / tLen;
                prevT = nextT;
            }
            tVals[ti] = 1;
            return (myTVals = tVals);
        }

        public void setTVals(float[] newTVals) {
            final float[] oldCoordinates = bezierCoordinates;
            final float[] newCoordinates = new float[2 + (newTVals.length - 1) * 6];
            final float[] oldTVals = getTVals();
            int oldCi = 0;
            float x0, xc0, xc1, x1;
            float y0, yc0, yc1, y1;
            x0 = xc0 = xc1 = x1 = oldCoordinates[oldCi++];
            y0 = yc0 = yc1 = y1 = oldCoordinates[oldCi++];
            int newCi = 0;
            newCoordinates[newCi++] = x0;
            newCoordinates[newCi++] = y0;
            float t0 = 0;
            float t1 = 0;
            int oldTi = 1;
            int newTi = 1;
            while (newTi < newTVals.length) {
                if (t0 >= t1) {
                    x0 = x1;
                    y0 = y1;
                    xc0 = oldCoordinates[oldCi++];
                    yc0 = oldCoordinates[oldCi++];
                    xc1 = oldCoordinates[oldCi++];
                    yc1 = oldCoordinates[oldCi++];
                    x1 = oldCoordinates[oldCi++];
                    y1 = oldCoordinates[oldCi++];
                    t1 = oldTVals[oldTi++];
                }
                float nt = newTVals[newTi++];
                // assert(nt > t0);
                if (nt < t1) {
                    // Make nt proportional to [t0 => t1] range
                    float relT = (nt - t0) / (t1 - t0);
                    newCoordinates[newCi++] = x0 = interp(x0, xc0, relT);
                    newCoordinates[newCi++] = y0 = interp(y0, yc0, relT);
                    xc0 = interp(xc0, xc1, relT);
                    yc0 = interp(yc0, yc1, relT);
                    xc1 = interp(xc1, x1, relT);
                    yc1 = interp(yc1, y1, relT);
                    newCoordinates[newCi++] = x0 = interp(x0, xc0, relT);
                    newCoordinates[newCi++] = y0 = interp(y0, yc0, relT);
                    xc0 = interp(xc0, xc1, relT);
                    yc0 = interp(yc0, yc1, relT);
                    newCoordinates[newCi++] = x0 = interp(x0, xc0, relT);
                    newCoordinates[newCi++] = y0 = interp(y0, yc0, relT);
                } else {
                    newCoordinates[newCi++] = xc0;
                    newCoordinates[newCi++] = yc0;
                    newCoordinates[newCi++] = xc1;
                    newCoordinates[newCi++] = yc1;
                    newCoordinates[newCi++] = x1;
                    newCoordinates[newCi++] = y1;
                }
                t0 = nt;
            }
            bezierCoordinates = newCoordinates;
            numCoordinates = newCoordinates.length;
            myTVals = newTVals;
        }
    }

    private static class MorphedShape implements Shape {
        final Geometry geom0;
        final Geometry geom1;
        final float t;
        final boolean unionBounds;

        MorphedShape(Geometry geom0, Geometry geom1, float t, boolean unionBounds) {
            this.geom0 = geom0;
            this.geom1 = geom1;
            this.t = t;
            this.unionBounds = unionBounds;
        }

        public Rectangle getBounds() {
            return getBounds2D().getBounds();
        }

        public Rectangle2D getBounds2D() {
            final int n = geom0.getNumCoordinates();
            float xMin, yMin, xMax, yMax;

            if (unionBounds) {
                xMin = xMax = geom0.getCoordinate(0);
                yMin = yMax = geom0.getCoordinate(1);
                for (int i = 2; i < n; i += 2) {
                    final float x = geom0.getCoordinate(i);
                    final float y = geom0.getCoordinate(i + 1);
                    if (xMin > x) {
                        xMin = x;
                    }
                    if (yMin > y) {
                        yMin = y;
                    }
                    if (xMax < x) {
                        xMax = x;
                    }
                    if (yMax < y) {
                        yMax = y;
                    }
                }
                final int m = geom1.getNumCoordinates();
                for (int i = 0; i < m; i += 2) {
                    final float x = geom1.getCoordinate(i);
                    final float y = geom1.getCoordinate(i + 1);
                    if (xMin > x) {
                        xMin = x;
                    }
                    if (yMin > y) {
                        yMin = y;
                    }
                    if (xMax < x) {
                        xMax = x;
                    }
                    if (yMax < y) {
                        yMax = y;
                    }
                }
            } else {
                xMin = xMax = interp(geom0.getCoordinate(0), geom1.getCoordinate(0), t);
                yMin = yMax = interp(geom0.getCoordinate(1), geom1.getCoordinate(1), t);
                for (int i = 2; i < n; i += 2) {
                    final float x = interp(geom0.getCoordinate(i), geom1.getCoordinate(i), t);
                    final float y = interp(geom0.getCoordinate(i + 1), geom1.getCoordinate(i + 1), t);
                    if (xMin > x) {
                        xMin = x;
                    }
                    if (yMin > y) {
                        yMin = y;
                    }
                    if (xMax < x) {
                        xMax = x;
                    }
                    if (yMax < y) {
                        yMax = y;
                    }
                }
            }
            return new Rectangle2D.Float(xMin, yMin, xMax - xMin, yMax - yMin);
        }

        public boolean contains(double x, double y) {
            return Path2D.contains(getPathIterator(null), x, y);
        }

        public boolean contains(Point2D p) {
            return Path2D.contains(getPathIterator(null), p);
        }

        public boolean intersects(double x, double y, double w, double h) {
            return Path2D.intersects(getPathIterator(null), x, y, w, h);
        }

        public boolean intersects(Rectangle2D r) {
            return Path2D.intersects(getPathIterator(null), r);
        }

        public boolean contains(double x, double y, double width, double height) {
            return Path2D.contains(getPathIterator(null), x, y, width, height);
        }

        public boolean contains(Rectangle2D r) {
            return Path2D.contains(getPathIterator(null), r);
        }

        public PathIterator getPathIterator(AffineTransform at) {
            return new Iterator(at, geom0, geom1, t);
        }

        public PathIterator getPathIterator(AffineTransform at, double flatness) {
            return new FlatteningPathIterator(getPathIterator(at), flatness);
        }
    }

    private static class Iterator implements PathIterator {
        AffineTransform at;
        Geometry g0;
        Geometry g1;
        float t;
        int cIndex;

        public Iterator(AffineTransform at,
                        Geometry g0, Geometry g1,
                        float t) {
            this.at = at;
            this.g0 = g0;
            this.g1 = g1;
            this.t = t;
        }

        /**
         * @{inheritDoc}
         */
        public int getWindingRule() {
            return (t < 0.5 ? g0.getWindingRule() : g1.getWindingRule());
        }

        /**
         * @{inheritDoc}
         */
        public boolean isDone() {
            return (cIndex > g0.getNumCoordinates());
        }

        /**
         * @{inheritDoc}
         */
        public void next() {
            if (cIndex == 0) {
                cIndex = 2;
            } else {
                cIndex += 6;
            }
        }

        /**
         * @{inheritDoc}
         */
        public int currentSegment(float[] coordinates) {
            int type;
            int n;
            if (cIndex == 0) {
                type = SEG_MOVETO;
                n = 2;
            } else if (cIndex >= g0.getNumCoordinates()) {
                type = SEG_CLOSE;
                n = 0;
            } else {
                type = SEG_CUBICTO;
                n = 6;
            }
            if (n > 0) {
                for (int i = 0; i < n; i++) {
                    coordinates[i] = interp(
                            g0.getCoordinate(cIndex + i),
                            g1.getCoordinate(cIndex + i),
                            t);
                }
                if (at != null) {
                    at.transform(coordinates, 0, coordinates, 0, n / 2);
                }
            }
            return type;
        }

        public int currentSegment(double[] coordinates) {
            final float[] temp = new float[6];
            final int res = currentSegment(temp);
            for (int i = 0; i < 6; i++) {
                coordinates[i] = temp[i];
            }
            return res;
        }
    }
}