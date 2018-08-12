package com.boydti.fawe.object.brush.sweep;

import com.google.common.base.Preconditions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.math.interpolation.Interpolation;

import java.util.ArrayList;
import java.util.List;

/**
 * Embodies an abstract implementation for pasting structures along a spline.<br>
 * A curve is being interpolated by the provided {@link Interpolation} implementation
 * and the structure is pasted along this curve by the specific Spline implementation.
 * @author Schuwi
 * @version 1.0
 */
public abstract class Spline {

    private Vector2D direction = new Vector2D(1, 0);
    private final int nodeCount;

    protected EditSession editSession;
    private Interpolation interpolation;

    private List<Section> sections;
    private double splineLength;

    /**
     * Constructor without position-correction. Use this constructor for an interpolation implementation which does not need position-correction.
     * <p>
     * Be advised that currently subsequent changes to the interpolation parameters may not be supported.
     * @param editSession     The EditSession which will be used when pasting the structure
     * @param interpolation   An implementation of the interpolation algorithm used to calculate the curve
     */
    protected Spline(EditSession editSession, Interpolation interpolation) {
        this(editSession, interpolation, -1);
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
     * @param editSession     The EditSession which will be used when pasting the structure
     * @param interpolation   An implementation of the interpolation algorithm used to calculate the curve
     * @param nodeCount       The number of nodes provided to the interpolation object
     */
    protected Spline(EditSession editSession, Interpolation interpolation, int nodeCount) {
        this.editSession = editSession;
        this.interpolation = interpolation;
        this.nodeCount = nodeCount;

        this.splineLength = interpolation.arcLength(0D, 1D);
        if (nodeCount > 2) {
            initSections();
        }
    }

    /**
     * Set the forward direction of the structure.<br>
     * This direction is used to determine the rotation of the clipboard to align to the curve. The horizontal slope
     * of the curve for a specific point is calculated by {@link Interpolation#get1stDerivative(double)}.
     * Subsequently this angle between this vector and the gradient vector is calculated and the clipboard content
     * is rotated by that angle to follow the curve slope.
     * <p>
     * The default direction is a (1;0) vector (pointing in the positive x-direction).
     * @param direction A normalized vector representing the horizontal forward direction of the clipboard content
     */
    public void setDirection(Vector2D direction) {
        this.direction = direction;
    }

    /**
     * Get the forward direction of the structure.<br>
     * This direction is used to determine the rotation of the clipboard to align to the curve. The horizontal slope
     * of the curve for a specific point is calculated by {@link Interpolation#get1stDerivative(double)}.
     * Subsequently this angle between this vector and the gradient vector is calculated and the clipboard content
     * is rotated by that angle to follow the curve slope.
     * <p>
     * The default direction is a (1;0) vector (pointing in the positive x-direction).
     * @return A vector representing the horizontal forward direction of the clipboard content
     */
    public Vector2D getDirection() {
        return direction;
    }

    /**
     * Paste the structure at the provided position on the curve. The position will be position-corrected if the
     * nodeCount provided to the constructor is bigger than 2.
     * @param position The position on the curve. Must be between 0.0 and 1.0 (both inclusive)
     * @return         The amount of blocks that have been changed
     * @throws MaxChangedBlocksException Thrown by WorldEdit if the limit of block changes for the {@link EditSession} has been reached
     */
    public int pastePosition(double position) throws MaxChangedBlocksException {
        Preconditions.checkArgument(position >= 0);
        Preconditions.checkArgument(position <= 1);

        if (nodeCount > 2) {
            return pastePositionDirect(translatePosition(position));
        } else {
            return pastePositionDirect(position);
        }
    }

    /**
     * Paste structure at the provided position on the curve. The position will not be position-corrected
     * but will be passed directly to the interpolation algorithm.
     * @param position The position on the curve. Must be between 0.0 and 1.0 (both inclusive)
     * @return         The amount of blocks that have been changed
     * @throws MaxChangedBlocksException Thrown by WorldEdit if the limit of block changes for the {@link EditSession} has been reached
     */
    public int pastePositionDirect(double position) throws MaxChangedBlocksException {
        Preconditions.checkArgument(position >= 0);
        Preconditions.checkArgument(position <= 1);

        // Calculate position from spline
        Vector target = interpolation.getPosition(position);
        Vector offset = target.subtract(target.round());
        target = target.subtract(offset);

        // Calculate rotation from spline

        Vector deriv = interpolation.get1stDerivative(position);
        Vector2D deriv2D = new Vector2D(deriv.getX(), deriv.getZ()).normalize();
        double angle = Math.toDegrees(
                Math.atan2(direction.getZ(), direction.getX()) - Math.atan2(deriv2D.getZ(), deriv2D.getX())
        );

        return pasteBlocks(target, offset, angle);
    }

    protected abstract int pasteBlocks(Vector target, Vector offset, double angle) throws MaxChangedBlocksException;

    private void initSections() {
        int sectionCount = nodeCount - 1;
        sections = new ArrayList<>(sectionCount);
        double sectionLength = 1D / sectionCount;

        double position = 0;
        for (int i = 0; i < sectionCount; i++) {
            double length;
            if (i == sectionCount - 1) { // maybe unnecessary precaution
                length = interpolation.arcLength(i * sectionLength, 1D) / splineLength;
            } else {
                length = interpolation.arcLength(i * sectionLength, (i + 1) * sectionLength) / splineLength;
            }
            sections.add(new Section(i * sectionLength, sectionLength, position, length));
            position += length;
        }
    }

    private double translatePosition(double flexPosition) {
        Section previousSection = sections.get(0); // start with first section
        for (int i = 1; i < sections.size(); i++) {
            Section section = sections.get(i);
            if (flexPosition < section.flexStart) {
                // break if the desired position is to the left of the current section -> the previous section contained the position
                break;
            }
            previousSection = section;
        }

        double flexOffset = flexPosition - previousSection.flexStart;
        double uniOffset = flexOffset / previousSection.flexLength * previousSection.uniLength;

        return previousSection.uniStart + uniOffset;
    }

    private class Section {
        final double uniStart;
        final double uniLength;
        final double flexStart;
        final double flexLength;

        Section(double uniStart, double uniLength, double flexStart, double flexLength) {
            this.uniStart = uniStart;
            this.uniLength = uniLength;
            this.flexStart = flexStart;
            this.flexLength = flexLength;
        }
    }
}