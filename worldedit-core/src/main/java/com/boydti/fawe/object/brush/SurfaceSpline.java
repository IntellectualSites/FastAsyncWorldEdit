package com.boydti.fawe.object.brush;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.brush.visualization.VisualExtent;
import com.boydti.fawe.object.collection.LocalBlockVectorSet;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.interpolation.KochanekBartelsInterpolation;
import com.sk89q.worldedit.math.interpolation.Node;
import java.util.ArrayList;
import java.util.List;

public class SurfaceSpline implements Brush {
    final double tension, bias, continuity, quality;

    public SurfaceSpline(final double tension, final double bias, final double continuity, final double quality) {
        this.tension = tension;
        this.bias = bias;
        this.continuity = continuity;
        this.quality = quality;
    }

    private ArrayList<Vector3> path = new ArrayList<>();

    @Override
    public void build(EditSession editSession, BlockVector3 pos, Pattern pattern, double radius) throws MaxChangedBlocksException {
        int maxY = editSession.getMaxY();
        boolean vis = editSession.getExtent() instanceof VisualExtent;
        if (path.isEmpty() || !pos.equals(path.get(path.size() - 1))) {
            int max = editSession.getNearestSurfaceTerrainBlock(pos.getBlockX(), pos.getBlockZ(), pos.getBlockY(), 0, editSession.getMaxY());
            if (max == -1) return;
//            pos.mutY(max);
            path.add(new Vector3(pos.getBlockX(), max, pos.getBlockZ()));
            editSession.getPlayer().sendMessage(BBC.getPrefix() + BBC.BRUSH_SPLINE_PRIMARY_2.s());
            if (!vis) return;
        }
        LocalBlockVectorSet vset = new LocalBlockVectorSet();
        final List<Node> nodes = new ArrayList<>(path.size());
        final KochanekBartelsInterpolation interpol = new KochanekBartelsInterpolation();

        for (final Vector3 nodevector : path) {
            final Node n = new Node(nodevector);
            n.setTension(tension);
            n.setBias(bias);
            n.setContinuity(continuity);
            nodes.add(n);
        }
        interpol.setNodes(nodes);
        final double splinelength = interpol.arcLength(0, 1);
        for (double loop = 0; loop <= 1; loop += 1D / splinelength / quality) {
            final Vector3 tipv = interpol.getPosition(loop);
            final int tipx = MathMan.roundInt(tipv.getX());
            final int tipz = (int) tipv.getZ();
            int tipy = MathMan.roundInt(tipv.getY());
            tipy = editSession.getNearestSurfaceTerrainBlock(tipx, tipz, tipy, 0, maxY);
            if (tipy == -1) continue;
            if (radius == 0) {
            	BlockVector3 set = MutableBlockVector.get(tipx, tipy, tipz).toBlockVector3();
                try {
                    pattern.apply(editSession, set, set);
                } catch (WorldEditException e) {
                    e.printStackTrace();
                }
            } else {
                vset.add(tipx, tipy, tipz);
            }
        }
        if (radius != 0) {
            double radius2 = (radius * radius);
            LocalBlockVectorSet newSet = new LocalBlockVectorSet();
            final int ceilrad = (int) Math.ceil(radius);
            for (final BlockVector3 v : vset) {
                final int tipx = v.getBlockX(), tipy = v.getBlockY(), tipz = v.getBlockZ();
                for (int loopx = tipx - ceilrad; loopx <= (tipx + ceilrad); loopx++) {
                    for (int loopz = tipz - ceilrad; loopz <= (tipz + ceilrad); loopz++) {
                        if (MathMan.hypot2(loopx - tipx, 0, loopz - tipz) <= radius2) {
                            int y = editSession.getNearestSurfaceTerrainBlock(loopx, loopz, v.getBlockY(), 0, maxY);
                            if (y == -1) continue;
                            newSet.add(loopx, y, loopz);
                        }
                    }
                }
            }
            editSession.setBlocks(newSet, pattern);
            if (!vis) path.clear();
        }
        editSession.getPlayer().sendMessage(BBC.getPrefix() + BBC.BRUSH_SPLINE_SECONDARY.s());
    }
}
