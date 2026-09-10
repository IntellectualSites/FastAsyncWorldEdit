package com.fastasyncworldedit.core.command.tool.brush;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.math.LocalBlockVectorSet;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.collection.BlockVector3Set;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.interpolation.KochanekBartelsInterpolation;
import com.sk89q.worldedit.math.interpolation.Node;

import java.util.ArrayList;
import java.util.List;

public class SurfaceSpline implements Brush {

    private final double tension;
    private final double bias;
    private final double continuity;
    private final double quality;
    private final ArrayList<BlockVector3> path = new ArrayList<>();

    public SurfaceSpline(double tension, double bias, double continuity, double quality) {
        this.tension = tension;
        this.bias = bias;
        this.continuity = continuity;
        this.quality = quality;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 pos, Pattern pattern, double radius) throws
            MaxChangedBlocksException {
        int maxY = editSession.getMaxY();
        int minY = editSession.getMinY();
        if (path.isEmpty() || !pos.equals(path.get(path.size() - 1))) {
            int max = editSession.getNearestSurfaceTerrainBlock(
                    pos.x(),
                    pos.z(),
                    pos.y(),
                    minY,
                    maxY
            );
            if (max == -1) {
                return;
            }
            path.add(BlockVector3.at(pos.x(), max, pos.z()));
            if (editSession.getActor() != null) {
                editSession.getActor().print(Caption.of("fawe.worldedit.brush.spline.primary.2"));
            }
            return;
        }
        final List<Node> nodes = new ArrayList<>(path.size());
        final KochanekBartelsInterpolation interpol = new KochanekBartelsInterpolation();

        for (BlockVector3 nodevector : path) {
            final Node n = new Node(nodevector.toVector3());
            n.setTension(tension);
            n.setBias(bias);
            n.setContinuity(continuity);
            nodes.add(n);
        }
        MutableBlockVector3 mutable = MutableBlockVector3.at(0, 0, 0);
        interpol.setNodes(nodes);
        final double splinelength = interpol.arcLength(0, 1);
        BlockVector3Set vset = LocalBlockVectorSet.wrapped();
        for (double loop = 0; loop <= 1; loop += 1D / splinelength / quality) {
            final Vector3 tipv = interpol.getPosition(loop);
            final int tipx = MathMan.roundInt(tipv.x());
            final int tipz = (int) tipv.z();
            int tipy = MathMan.roundInt(tipv.y());
            tipy = editSession.getNearestSurfaceTerrainBlock(tipx, tipz, tipy, minY, maxY, Integer.MIN_VALUE, Integer.MAX_VALUE);
            if (tipy == Integer.MIN_VALUE || tipy == Integer.MAX_VALUE) {
                continue;
            }
            if (radius == 0) {
                BlockVector3 set = mutable.setComponents(tipx, tipy, tipz);
                pattern.apply(editSession, set, set);
            } else {
                vset.add(tipx, tipy, tipz);
            }
        }
        if (radius != 0) {
            double radius2 = radius * radius;
            BlockVector3Set newSet = LocalBlockVectorSet.wrapped();
            final int ceilrad = (int) Math.ceil(radius);
            for (BlockVector3 v : vset) {
                final int tipx = v.x();
                final int tipz = v.z();
                for (int loopx = tipx - ceilrad; loopx <= tipx + ceilrad; loopx++) {
                    for (int loopz = tipz - ceilrad; loopz <= tipz + ceilrad; loopz++) {
                        if (MathMan.hypot2(loopx - tipx, 0, loopz - tipz) <= radius2) {
                            int y = editSession.getNearestSurfaceTerrainBlock(
                                    loopx,
                                    loopz,
                                    v.y(),
                                    minY,
                                    maxY,
                                    Integer.MIN_VALUE,
                                    Integer.MAX_VALUE
                            );
                            if (y == Integer.MIN_VALUE || y == Integer.MAX_VALUE) {
                                continue;
                            }
                            newSet.add(loopx, y, loopz);
                        }
                    }
                }
            }
            editSession.setBlocks(newSet, pattern);
        }
        path.clear();
        if (editSession.getActor() != null) {
            editSession.getActor().print(Caption.of("fawe.worldedit.brush.spline.secondary"));
        }
    }

}
