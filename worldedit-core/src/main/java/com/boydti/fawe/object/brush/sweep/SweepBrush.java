package com.boydti.fawe.object.brush.sweep;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.brush.ResettableTool;
import com.boydti.fawe.object.brush.visualization.VisualExtent;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.interpolation.Interpolation;
import com.sk89q.worldedit.math.interpolation.KochanekBartelsInterpolation;
import com.sk89q.worldedit.math.interpolation.Node;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SweepBrush implements Brush, ResettableTool {
    private List<Vector> positions;
    private Vector position;
    private int copies;

    private static final double tension = 0D;
    private static final double bias = 0D;
    private static final double continuity = 0D;

    public SweepBrush(int copies) {
        this.positions = new ArrayList<>();
        this.copies = copies > 0 ? copies : -1;
    }

    @Override
    public void build(EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        boolean visualization = editSession.getExtent() instanceof VisualExtent;
        if (visualization && positions.isEmpty()) {
            return;
        }

        boolean newPos = this.position == null || !position.equals(this.position);
        this.position = position;
        FawePlayer player = editSession.getPlayer();
        if (newPos) {
            BBC.BRUSH_SPLINE_PRIMARY_2.send(player);
            positions.add(position);
            return;
        }

        if (positions.size() < 2) {
            BBC.BRUSH_SPLINE_SECONDARY_ERROR.send(player);
            return;
        }

        Interpolation interpol = new KochanekBartelsInterpolation();
        List<Node> nodes = positions.stream().map(v -> {
            Node n = new Node(v);
            n.setTension(tension);
            n.setBias(bias);
            n.setContinuity(continuity);
            return n;
        }).collect(Collectors.toList());
        interpol.setNodes(nodes);

        LocalSession session = player.getSession();
        ClipboardHolder holder = session.getExistingClipboard();
        if (holder == null) {
            throw new RuntimeException(new EmptyClipboardException());
        }
        Clipboard clipboard = holder.getClipboard();

        Vector dimensions = clipboard.getDimensions();
        AffineTransform transform = new AffineTransform();
        if (dimensions.getBlockX() > dimensions.getBlockZ()) {
            transform = transform.rotateY(90);
        }
        double quality = Math.max(dimensions.getBlockX(), dimensions.getBlockZ());

        ClipboardSpline spline = new ClipboardSpline(editSession, holder, interpol, transform, nodes.size());

        switch (copies) {
            case 1: {
                spline.pastePosition(0D);
                break;
            }
            case -1: {
                double splineLength = interpol.arcLength(0D, 1D);
                double blockDistance = 1d / splineLength;
                double step = blockDistance / quality;
                double accumulation = 0;
                Vector last = null;
                for (double pos = 0D; pos <= 1D; pos += step) {
                    Vector gradient = interpol.get1stDerivative(pos);
                    if (last == null) last = new Vector(interpol.get1stDerivative(pos));
                    double dist = MathMan.sqrtApprox(last.distanceSq(gradient));
                    last.mutX(gradient.getX());
                    last.mutY(gradient.getY());
                    last.mutZ(gradient.getZ());
                    double change = dist * step;
                    // Accumulation is arbitrary, but much faster than calculation overlapping regions
                    if ((accumulation += change + step * 2) > blockDistance) {
                        accumulation -= blockDistance;
                        spline.pastePosition(pos);
                    }
                }
                break;
            }
            default: {
                for (double pos = 0D; pos <= 1D; pos += 1D / (copies -  1)) {
                    spline.pastePosition(pos);
                }
                break;
            }
        }
        BBC.BRUSH_SPLINE_SECONDARY.send(player);
        reset();
    }

    @Override
    public boolean reset() {
        positions.clear();
        position = null;
        return true;
    }
}
