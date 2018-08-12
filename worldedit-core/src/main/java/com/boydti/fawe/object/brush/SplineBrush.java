package com.boydti.fawe.object.brush;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.brush.visualization.VisualExtent;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.mask.IdMask;
import com.boydti.fawe.object.visitor.DFSRecursiveVisitor;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.interpolation.Node;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SplineBrush implements Brush, ResettableTool {

    public static int MAX_POINTS = 15;
    private ArrayList<ArrayList<Vector>> positionSets;
    private int numSplines;

    private final LocalSession session;
    private final Player player;
    private Vector position;

    public SplineBrush(Player player, LocalSession session) {
        this.session = session;
        this.player = player;
        this.positionSets = new ArrayList<>();
    }


    @Override
    public boolean reset() {
        numSplines = 0;
        positionSets.clear();
        position = null;
        return true;
    }

    @Override
    public void build(EditSession editSession, final Vector position, Pattern pattern, double size) throws WorldEditException {
        Mask mask = editSession.getMask();
        if (mask == null) {
            mask = new IdMask(editSession);
        } else {
            mask = new MaskIntersection(mask, new IdMask(editSession));
        }
        boolean visualization = editSession.getExtent() instanceof VisualExtent;
        if (visualization && positionSets.isEmpty()) {
            return;
        }
        int originalSize = numSplines;
        boolean newPos = this.position == null || !position.equals(this.position);
        this.position = position;
        if (newPos) {
            if (positionSets.size() >= MAX_POINTS) {
                throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_CHECKS);
            }
            final ArrayList<Vector> points = new ArrayList<>();
            if (size > 0) {
                DFSRecursiveVisitor visitor = new DFSRecursiveVisitor(mask, new RegionFunction() {
                    @Override
                    public boolean apply(Vector p) {
                        points.add(new Vector(p));
                        return true;
                    }
                }, (int) size, 1);
                List<Vector> directions = visitor.getDirections();
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x != 0 || y != 0 || z != 0) {
                                Vector pos = new Vector(x, y, z);
                                if (!directions.contains(pos)) {
                                    directions.add(pos);
                                }
                            }
                        }
                    }
                }
                Collections.sort(directions, (o1, o2) -> (int) Math.signum(o1.lengthSq() - o2.lengthSq()));
                visitor.visit(position);
                Operations.completeBlindly(visitor);
                if (points.size() > numSplines) {
                    numSplines = points.size();
                }
            } else {
                points.add(position);
            }
            this.positionSets.add(points);
            player.print(BBC.getPrefix() + BBC.BRUSH_SPLINE_PRIMARY_2.s());
            if (!visualization) {
                return;
            }
        }
        if (positionSets.size() < 2) {
            player.print(BBC.getPrefix() + BBC.BRUSH_SPLINE_SECONDARY_ERROR.s());
            return;
        }
        List<Vector> centroids = new ArrayList<>();
        for (List<Vector> points : positionSets) {
            centroids.add(getCentroid(points));
        }

        double tension = 0;
        double bias = 0;
        double continuity = 0;
        double quality = 10;

        final List<Node> nodes = new ArrayList<Node>(centroids.size());

        for (final Vector nodevector : centroids) {
            final Node n = new Node(nodevector);
            n.setTension(tension);
            n.setBias(bias);
            n.setContinuity(continuity);
            nodes.add(n);
        }
        int samples = numSplines;
        for (int i = 0; i < numSplines; i++) {
            List<Vector> currentSpline = new ArrayList<>();
            for (ArrayList<Vector> points : positionSets) {
                int listSize = points.size();
                int index = (int) (i * listSize / (double) (numSplines));
                currentSpline.add(points.get(index));
            }
            editSession.drawSpline(pattern, currentSpline, 0, 0, 0, 10, 0, true);
        }
        player.print(BBC.getPrefix() + BBC.BRUSH_SPLINE_SECONDARY.s());
        if (visualization) {
            numSplines = originalSize;
            positionSets.remove(positionSets.size() - 1);
        } else {
            positionSets.clear();
            numSplines = 0;
        }
    }

    private Vector getCentroid(Collection<Vector> points) {
        Vector sum = new Vector();
        for (Vector p : points) {
            sum.mutX(sum.getX() + p.getX());
            sum.mutY(sum.getY() + p.getY());
            sum.mutZ(sum.getZ() + p.getZ());
        }
        return sum.multiply(1.0 / points.size());
    }

    private Vector normal(Collection<Vector> points, Vector centroid) {
        int n = points.size();
        switch (n) {
            case 1: {
                return null;
            }
            case 2: {
                return null;
            }
        }

        // Calc full 3x3 covariance matrix, excluding symmetries:
        double xx = 0.0;
        double xy = 0.0;
        double xz = 0.0;
        double yy = 0.0;
        double yz = 0.0;
        double zz = 0.0;

        Vector r = new Vector();
        for (Vector p : points) {
            r.mutX((p.getX() - centroid.getX()));
            r.mutY((p.getY() - centroid.getY()));
            r.mutZ((p.getZ() - centroid.getZ()));
            xx += r.getX() * r.getX();
            xy += r.getX() * r.getY();
            xz += r.getX() * r.getZ();
            yy += r.getY() * r.getY();
            yz += r.getY() * r.getZ();
            zz += r.getZ() * r.getZ();
        }

        double det_x = yy * zz - yz * yz;
        double det_y = xx * zz - xz * xz;
        double det_z = xx * yy - xy * xy;

        double det_max = Math.max(Math.max(det_x, det_y), det_z);
        if (det_max <= 0.0) {
            return null;
        }

        // Pick path with best conditioning:
        Vector dir;
        if (det_max == det_x) {
            double a = (xz * yz - xy * zz) / det_x;
            double b = (xy * yz - xz * yy) / det_x;
            dir = new Vector(1.0, a, b);
        } else if (det_max == det_y) {
            double a = (yz * xz - xy * zz) / det_y;
            double b = (xy * xz - yz * xx) / det_y;
            dir = new Vector(a, 1.0, b);
        } else {
            double a = (yz * xy - xz * yy) / det_z;
            double b = (xz * xy - yz * xx) / det_z;
            dir = new Vector(a, b, 1.0);
        }
        ;
        return dir.normalize();
    }
}
