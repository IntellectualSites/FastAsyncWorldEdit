package com.boydti.fawe.object.brush;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.brush.visualization.VisualExtent;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.mask.IdMask;
import com.boydti.fawe.object.visitor.DFSRecursiveVisitor;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.interpolation.Node;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SplineBrush implements Brush, ResettableTool {

    public static int MAX_POINTS = 15;
    private ArrayList<ArrayList<BlockVector3>> positionSets;
    private int numSplines;

    private final Player player;
    private BlockVector3 position;

    public SplineBrush(Player player) {
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
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws WorldEditException {
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
                throw FaweCache.MAX_CHECKS;
            }
            final ArrayList<BlockVector3> points = new ArrayList<>();
            if (size > 0) {
                DFSRecursiveVisitor visitor = new DFSRecursiveVisitor(mask, p -> {
                    points.add(p);
                    return true;
                }, (int) size, 1);
                List<BlockVector3> directions = visitor.getDirections();
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x != 0 || y != 0 || z != 0) {
                                BlockVector3 pos = BlockVector3.at(x, y, z);
                                if (!directions.contains(pos)) {
                                    directions.add(pos);
                                }
                            }
                        }
                    }
                }
                directions.sort((o1, o2) -> (int) Math.signum(o1.lengthSq() - o2.lengthSq()));
                visitor.visit(position);
                Operations.completeBlindly(visitor);
                if (points.size() > numSplines) {
                    numSplines = points.size();
                }
            } else {
                points.add(position);
            }
            this.positionSets.add(points);
            player.print(BBC.BRUSH_SPLINE_PRIMARY_2.s());
            if (!visualization) {
                return;
            }
        }
        if (positionSets.size() < 2) {
            player.print(BBC.BRUSH_SPLINE_SECONDARY_ERROR.s());
            return;
        }
        List<Vector3> centroids = new ArrayList<>();
        for (List<BlockVector3> points : positionSets) {
            centroids.add(getCentroid(points));
        }

        double tension = 0;
        double bias = 0;
        double continuity = 0;
        double quality = 10;

        final List<Node> nodes = new ArrayList<>(centroids.size());

        for (Vector3 nodevector : centroids) {
            final Node n = new Node(nodevector);
            n.setTension(tension);
            n.setBias(bias);
            n.setContinuity(continuity);
            nodes.add(n);
        }
        int samples = numSplines;
        for (int i = 0; i < numSplines; i++) {
            List<BlockVector3> currentSpline = new ArrayList<>();
            for (ArrayList<BlockVector3> points : positionSets) {
                int listSize = points.size();
                int index = (int) (i * listSize / (double) numSplines);
                currentSpline.add(points.get(index));
            }
            editSession.drawSpline(pattern, currentSpline, 0, 0, 0, 10, 0, true);
        }
        player.print(BBC.BRUSH_SPLINE_SECONDARY.s());
        if (visualization) {
            numSplines = originalSize;
            positionSets.remove(positionSets.size() - 1);
        } else {
            positionSets.clear();
            numSplines = 0;
        }
    }

    private Vector3 getCentroid(Collection<BlockVector3> points) {
        MutableVector3 sum = new MutableVector3();
        for (BlockVector3 p : points) {
            sum.mutX(sum.getX() + p.getX());
            sum.mutY(sum.getY() + p.getY());
            sum.mutZ(sum.getZ() + p.getZ());
        }
        return sum.multiply(1.0 / points.size());
    }

    private BlockVector3 normal(Collection<BlockVector3> points, BlockVector3 centroid) {
        int n = points.size();
        switch (n) {
            case 1:
                return null;
            case 2:
                return null;
        }

        // Calc full 3x3 covariance matrix, excluding symmetries:
        double xx = 0.0;
        double xy = 0.0;
        double xz = 0.0;
        double yy = 0.0;
        double yz = 0.0;
        double zz = 0.0;

        MutableVector3 r = new MutableVector3();
        for (BlockVector3 p : points) {
            r.mutX(p.getX() - centroid.getX());
            r.mutY(p.getY() - centroid.getY());
            r.mutZ(p.getZ() - centroid.getZ());
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
        BlockVector3 dir;
        if (det_max == det_x) {
            double a = (xz * yz - xy * zz) / det_x;
            double b = (xy * yz - xz * yy) / det_x;
            dir = BlockVector3.at(1.0, a, b);
        } else if (det_max == det_y) {
            double a = (yz * xz - xy * zz) / det_y;
            double b = (xy * xz - yz * xx) / det_y;
            dir = BlockVector3.at(a, 1.0, b);
        } else {
            double a = (yz * xy - xz * yy) / det_z;
            double b = (xz * xy - yz * xx) / det_z;
            dir = BlockVector3.at(a, b, 1.0);
        }
        return dir.normalize();
    }
}
