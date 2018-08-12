package com.boydti.fawe.object.brush.sweep;

import com.boydti.fawe.object.collection.LocalBlockVectorSet;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.interpolation.Interpolation;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.RoundedTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;

/**
 * An implementation of a {@link Spline} using a Clipboard as source for the structure.
 * @author Schuwi
 * @version 1.0
 */
public class ClipboardSpline extends Spline {

    private final Transform transform;
    private ClipboardHolder clipboardHolder;
    private Vector originalOrigin;
    private Transform originalTransform;

    private Vector center;
    private Vector centerOffset;
    private LocalBlockVectorSet buffer;

    /**
     * Constructor without position-correction. Use this constructor for an interpolation implementation which does not need position-correction.
     * <p>
     * Be advised that currently subsequent changes to the interpolation parameters may not be supported.
     * @param editSession     The EditSession which will be used when pasting the clipboard content
     * @param clipboardHolder The clipboard that will be pasted along the spline
     * @param interpolation   An implementation of the interpolation algorithm used to calculate the curve
     */
    public ClipboardSpline(EditSession editSession, ClipboardHolder clipboardHolder, Interpolation interpolation) {
        this(editSession, clipboardHolder, interpolation, new AffineTransform(), -1);
    }

    /**
     * Constructor with position-correction. Use this constructor for an interpolation implementation that needs position-correction.
     * <p>
     * Some interpolation implementations calculate the position on the curve (used by {@link #pastePosition(double)})
     * based on an equidistant distribution of the nodes on the curve. For example: on a spline with 5 nodes position 0.0 would refer
     * to the first node, 0.25 to the second, 0.5 to the third, ... .<br>
     * By providing this method with the amount of nodes used by the interpolation implementation the distribution of the
     * nodes is converted to a proportional distribution based on the length between two adjacent nodes calculated by {@link Interpolation#arcLength(double, double)}.<br>
     * This means that the distance between two positions used to paste the clipboard (e.g. 0.75 - 0.5 = 0.25) on the curve
     * will always amount to that part of the length (e.g. 40 units) of the curve. In this example it would amount to
     * 0.25 * 40 = 10 units of curve length between these two positions.
     * <p>
     * Be advised that currently subsequent changes to the interpolation parameters may not be supported.
     * @param editSession     The EditSession which will be used when pasting the clipboard content
     * @param clipboardHolder The clipboard that will be pasted along the spline
     * @param interpolation   An implementation of the interpolation algorithm used to calculate the curve
     * @param nodeCount       The number of nodes provided to the interpolation object
     */
    public ClipboardSpline(EditSession editSession, ClipboardHolder clipboardHolder, Interpolation interpolation, Transform transform, int nodeCount) {
        super(editSession, interpolation, nodeCount);
        this.clipboardHolder = clipboardHolder;

        this.originalTransform = clipboardHolder.getTransform();
        Clipboard clipboard = clipboardHolder.getClipboard();
        this.originalOrigin = clipboard.getOrigin();

        Region region = clipboard.getRegion();
        Vector origin = clipboard.getOrigin();
        center = region.getCenter().setY(origin.getY() - 1);
        this.centerOffset = center.subtract(center.round());
        this.center = center.subtract(centerOffset);
        this.transform = transform;
        this.buffer = new LocalBlockVectorSet();
    }

    @Override
    protected int pasteBlocks(Vector target, Vector offset, double angle) throws MaxChangedBlocksException {
        RoundedTransform transform = new RoundedTransform(new AffineTransform()
                .translate(offset)
                .rotateY(angle));
        if (!this.transform.isIdentity()) {
            transform = transform.combine(this.transform);
        }
        if (!originalTransform.isIdentity()) {
            transform = transform.combine(originalTransform);
        }

        // Pasting
        Clipboard clipboard = clipboardHolder.getClipboard();
        clipboard.setOrigin(center.subtract(centerOffset).round());
        clipboardHolder.setTransform(transform);

        Vector functionOffset = target.subtract(clipboard.getOrigin());
        final int offX = functionOffset.getBlockX();
        final int offY = functionOffset.getBlockY();
        final int offZ = functionOffset.getBlockZ();

        Operation operation = clipboardHolder
                .createPaste(editSession)
                .to(target)
                .ignoreAirBlocks(true)
                .filter(v -> buffer.add(v.getBlockX() + offX, v.getBlockY() + offY, v.getBlockZ() + offZ))
                .build();
        Operations.completeLegacy(operation);

        // Cleanup
        clipboardHolder.setTransform(originalTransform);
        clipboard.setOrigin(originalOrigin);

        return operation instanceof ForwardExtentCopy ? ((ForwardExtentCopy) operation).getAffected() : 0;
    }
}