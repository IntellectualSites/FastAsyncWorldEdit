package com.boydti.fawe.object.regions;

import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.StringMan;
import com.google.common.base.Preconditions;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.polyhedron.Edge;

public class Triangle {

    public static double RADIUS = 0.5;

    private final double[][] verts = new double[3][3];
    private final double[] center = new double[3];
    private final double[] radius = new double[3];
    private final double[] v0 = new double[3];
    private final double[] v1 = new double[3];
    private final double[] v2 = new double[3];
    private final double[] normal = new double[3];
    private final double[] e0 = new double[3];
    private final double[] e1 = new double[3];
    private final double[] e2 = new double[3];
    private final double[] vmin = new double[3];
    private final double[] vmax = new double[3];

    private final BlockVector3 normalVec;
    private final double b;

    public Triangle(BlockVector3 pos1, BlockVector3 pos2, BlockVector3 pos3) {
        verts[0] = new double[]{pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ()};
        verts[1] = new double[]{pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ()};
        verts[2] = new double[]{pos3.getBlockX(), pos3.getBlockY(), pos3.getBlockZ()};
        radius[0] = RADIUS;
        radius[1] = RADIUS;
        radius[2] = RADIUS;
        this.normalVec = pos2.subtract(pos1).cross(pos3.subtract(pos1)).normalize();
        this.b = Math.max(Math.max(this.normalVec.dot(pos1), this.normalVec.dot(pos2)), this.normalVec.dot(pos3));
    }

    public boolean above(BlockVector3 pt) {
        Preconditions.checkNotNull(pt);
        return this.normalVec.dot(pt) > this.b;
    }

    public Edge getEdge(int index) {
        if (index == this.verts.length - 1) {
            return new Edge(Vector3.at(this.verts[index][0], this.verts[index][1],this.verts[index][2]), Vector3.at(this.verts[0][0], this.verts[0][1], this.verts[0][2]));
        } else {
            return new Edge(Vector3.at(this.verts[index][0], this.verts[index][1],this.verts[index][2]), Vector3.at(this.verts[index + 1][0], this.verts[index + 1][1], this.verts[index + 1][2]));
        }
    }

    @Override
    public String toString() {
        return StringMan.getString(verts);
    }

    public Vector3 getVertex(int index) {
        return Vector3.at(verts[index][0], verts[index][1], verts[index][2]);
    }

    public boolean contains(BlockVector3 pos) {
        center[0] = pos.getBlockX() + RADIUS;
        center[1] = pos.getBlockY() + RADIUS;
        center[2] = pos.getBlockZ() + RADIUS;
        return overlaps(center, radius, verts);
    }

    private void sub(double[] dest, double[] v1, double[] v2) {
        dest[0] = v1[0] - v2[0];
        dest[1] = v1[1] - v2[1];
        dest[2] = v1[2] - v2[2];
    }

    private void cross(double[] dest, double[] v1, double[] v2) {
        dest[0] = v1[1] * v2[2] - v1[2] * v2[1];
        dest[1] = v1[2] * v2[0] - v1[0] * v2[2];
        dest[2] = v1[0] * v2[1] - v1[1] * v2[0];
    }

    private double dot(double[] v1, double[] v2) {
        return (v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2]);
    }


    private boolean axisTestX01(double a, double b, double fa, double fb) {
        double p0 = a * v0[1] - b * v0[2];
        double p2 = a * v2[1] - b * v2[2];
        double min, max;
        if (p0 < p2) {
            min = p0;
            max = p2;
        } else {
            min = p2;
            max = p0;
        }
        double rad = fa * radius[1] + fb * radius[2];
        return !(min > rad || max < -rad);
    }

    private boolean axisTestX2(double a, double b, double fa, double fb) {
        double p0 = a * v0[1] - b * v0[2];
        double p1 = a * v1[1] - b * v1[2];
        double min, max;
        if (p0 < p1) {
            min = p0;
            max = p1;
        } else {
            min = p1;
            max = p0;
        }
        double rad = fa * radius[1] + fb * radius[2];
        return !(min > rad || max < -rad);
    }

    private boolean axisTestY02(double a, double b, double fa, double fb) {
        double p0 = -a * v0[0] + b * v0[2];
        double p2 = -a * v2[0] + b * v2[2];
        double min, max;
        if (p0 < p2) {
            min = p0;
            max = p2;
        } else {
            min = p2;
            max = p0;
        }
        double rad = fa * radius[0] + fb * radius[2];
        return !(min > rad || max < -rad);
    }

    private boolean axisTestY1(double a, double b, double fa, double fb) {
        double p0 = -a * v0[0] + b * v0[2];
        double p1 = -a * v1[0] + b * v1[2];
        double min, max;
        if (p0 < p1) {
            min = p0;
            max = p1;
        } else {
            min = p1;
            max = p0;
        }
        double rad = fa * radius[0] + fb * radius[2];
        return !(min > rad || max < -rad);
    }

    private boolean axisTestZ12(double a, double b, double fa, double fb) {
        double p1 = a * v1[0] - b * v1[1];
        double p2 = a * v2[0] - b * v2[1];
        double min, max;
        if (p2 < p1) {
            min = p2;
            max = p1;
        } else {
            min = p1;
            max = p2;
        }
        double rad = fa * radius[0] + fb * radius[1];
        return !(min > rad || max < -rad);
    }

    private boolean axisTestZ0(double a, double b, double fa, double fb) {
        double p0 = a * v0[0] - b * v0[1];
        double p1 = a * v1[0] - b * v1[1];
        double min, max;
        if (p0 < p1) {
            min = p0;
            max = p1;
        } else {
            min = p1;
            max = p0;
        }
        double rad = fa * radius[0] + fb * radius[1];
        return !(min > rad || max < -rad);
    }


    private boolean overlaps(double boxcenter[], double boxhalfsize[], double triverts[][]) {
        double min, max, p0, p1, p2, rad, fex, fey, fez;
        sub(v0, triverts[0], boxcenter);
        sub(v1, triverts[1], boxcenter);
        sub(v2, triverts[2], boxcenter);
        sub(e0, v1, v0);      /* tri edge 0 */
        sub(e1, v2, v1);      /* tri edge 1 */
        sub(e2, v0, v2);      /* tri edge 2 */

        fex = Math.abs(e0[0]);
        fey = Math.abs(e0[1]);
        fez = Math.abs(e0[2]);

        if (!axisTestX01(e0[2], e0[1], fez, fey)) return false;
        if (!axisTestY02(e0[2], e0[0], fez, fex)) return false;
        if (!axisTestZ12(e0[1], e0[0], fey, fex)) return false;

        fex = Math.abs(e1[0]);
        fey = Math.abs(e1[1]);
        fez = Math.abs(e1[2]);

        if (!axisTestX01(e1[2], e1[1], fez, fey)) return false;
        if (!axisTestY02(e1[2], e1[0], fez, fex)) return false;
        if (!axisTestZ0(e1[1], e1[0], fey, fex)) return false;


        fex = Math.abs(e2[0]);
        fey = Math.abs(e2[1]);
        fez = Math.abs(e2[2]);

        if (!axisTestX2(e2[2], e2[1], fez, fey)) return false;
        if (!axisTestY1(e2[2], e2[0], fez, fex)) return false;
        if (!axisTestZ12(e2[1], e2[0], fey, fex)) return false;

        max = MathMan.max(v0[0], v1[0], v2[0]);
        min = MathMan.min(v0[0], v1[0], v2[0]);

        if (min > boxhalfsize[0] || max < -boxhalfsize[0]) return false;

        max = MathMan.max(v0[1], v1[1], v2[1]);
        min = MathMan.min(v0[1], v1[1], v2[1]);

        if (min > boxhalfsize[1] || max < -boxhalfsize[1]) return false;

        max = MathMan.max(v0[2], v1[2], v2[2]);
        min = MathMan.min(v0[2], v1[2], v2[2]);

        if (min > boxhalfsize[2] || max < -boxhalfsize[2]) return false;

        cross(normal, e0, e1);

        return (planeBoxOverlap(normal, v0, boxhalfsize));
    }

    private boolean planeBoxOverlap(double normal[], double vert[], double maxbox[]) {
        for (int q = 0; q <= 2; q++) {
            double v = vert[q];
            if (normal[q] > 0.0f) {
                vmin[q] = -maxbox[q] - v;
                vmax[q] = maxbox[q] - v;
            } else {
                vmin[q] = maxbox[q] - v;
                vmax[q] = -maxbox[q] - v;
            }
        }
        if (dot(normal, vmin) > 0.0f) return false;
        if (dot(normal, vmax) >= 0.0f) return true;
        return false;
    }
}
