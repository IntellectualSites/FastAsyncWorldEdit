package com.boydti.fawe.object.brush.sweep;

import com.boydti.fawe.object.brush.ResettableTool;
import com.boydti.fawe.object.brush.visualization.VisualExtent;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.interpolation.Interpolation;
import com.sk89q.worldedit.math.interpolation.KochanekBartelsInterpolation;
import com.sk89q.worldedit.math.interpolation.Node;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SweepBrush implements Brush, ResettableTool {
    private List<BlockVector3> positions;
    private BlockVector3 position;
    private int copies;

    private static final double tension = 0D;
    private static final double bias = 0D;
    private static final double continuity = 0D;

    public SweepBrush(int copies) {
        this.positions = new ArrayList<>();
        this.copies = copies > 0 ? copies : -1;
    }

    private Vector3 getSidePos(Interpolation interpol, double position, Clipboard clipboard, boolean rightHand) {
        Vector3 pos = interpol.getPosition(position);
        Vector3 deriv = interpol.get1stDerivative(position);
        double length = (rightHand ? 1 : -1) * Math.abs(
            clipboard.getOrigin().getBlockZ() - clipboard.getRegion().getMinimumPoint().getBlockZ()
        );

        Vector3 cross = deriv.cross(Vector3.UNIT_Y);
        if (cross.equals(Vector3.ZERO)) {
            return null;
        }

        Vector3 perpendicular = cross.normalize().multiply(length);
        return pos.add(perpendicular);
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        boolean visualization = editSession.getExtent() instanceof VisualExtent;
        if (visualization && positions.isEmpty()) {
            return;
        }

        boolean newPos = !position.equals(this.position);
        this.position = position;
        Player player = editSession.getPlayer();
        if (player == null) {
            //TODO Insert Error Message here or modify EditSession to not require a player.
            return;
        }
        if (newPos) {
            player.print(TranslatableComponent.of("fawe.worldedit.brush.spline.primary.2"));
            positions.add(position);
            return;
        }

        if (positions.size() < 2) {
            player.printError(TranslatableComponent.of("fawe.worldedit.brush.brush.spline.secondary.error"));
            return;
        }

        Interpolation interpol = new KochanekBartelsInterpolation();
        List<Node> nodes = positions.stream().map(v -> {
            Node n = new Node(v.toVector3());
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

        BlockVector3 dimensions = clipboard.getDimensions();
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
                MutableVector3 lastLHS = new MutableVector3(getSidePos(interpol, 0, clipboard, false).round());
                MutableVector3 lastRHS = new MutableVector3(getSidePos(interpol, 0, clipboard, true).round());

                for (double pos = 0D; pos <= 1D; pos += step) {
                    Vector3 lhs = getSidePos(interpol, pos, clipboard, false);
                    boolean doLeft = lhs == null || !lhs.round().equals(lastLHS);
                    if (doLeft && lhs != null) {
                        lastLHS.setComponents(lhs.round());
                    }
                    
                    Vector3 rhs = getSidePos(interpol, pos, clipboard, true);
                    boolean doRight = rhs == null || !rhs.round().equals(lastRHS);
                    if (doRight && rhs != null) {
                        lastRHS.setComponents(rhs.round());
                    }
                    
                    // Only paste if the edges have passed the previous edges
                    if (doLeft || doRight) {
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
        player.print(TranslatableComponent.of("fawe.worldedit.brush.spline.secondary"));
        reset();
    }

    @Override
    public boolean reset() {
        positions.clear();
        position = null;
        return true;
    }
}
