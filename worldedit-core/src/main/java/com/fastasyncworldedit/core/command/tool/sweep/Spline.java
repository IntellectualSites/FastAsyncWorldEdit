package com.fastasyncworldedit.core.command.tool.sweep;

import com.google.common.base.Preconditions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.interpolation.Interpolation;

import java.util.ArrayList;
import java.util.List;

/**
 * Embodies an abstract implementation for pasting structures along a spline.<br>
 * A curve is being interpolated by the provided {@link Interpolation} implementation
 * and the structure is pasted along this curve by the specific Spline implementation.
 */
public abstract class Spline {

    private Vector2 direction = Vector2.at(1, 0);
    private final int nodeCount;

    protected EditSession editSession;
    private final Interpolation interpolation;

    private List<Section> sections;
    private final double splineLength;

    /**
     * Constructor without position-correction. Use this constructor for an interpolation implementation which does not need position-correction.
     * <p>
     * Be advised that currently subsequent changes to the interpolation parameters may not be supported.
     *
     * @param editSession   The EditSession which will be used when pasting the structure
     * @param interpolation An implementation of the interpolation algorithm used to calculate the curve
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
     * This means that the distance between two positions used to paste the clipboard (e.g., 0.75 - 0.5 = 0.25) on the curve
     * will always amount to that part of the length (e.g. 40 units) of the curve. In this example it would amount to
     * 0.25 * 40 = 10 units of curve length between these two positions.
     * <p>
     * Be advised that currently subsequent changes to the interpolation parameters may not be supported.
     *
     * @param editSession   The EditSession which will be used when pasting the structure
     * @param interpolation An implementation of the interpolation algorithm used to calculate the curve
     * @param nodeCount     The number of nodes provided to the interpolation object
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
     * Subsequently, this angle between this vector, and the gradient vector is calculated, and the clipboard content
     * is rotated by that angle to follow the curve slope.
     * <p>
     * The default direction is a (1;0) vector (pointing in the positive x-direction).
     *
     * @param direction A vector representing the horizontal forward direction of the clipboard content
     */
    public void setDirection(Vector2 direction) {
        this.direction = direction.normalize();
    }

    /**
     * Get the forward direction of the structure.<br>
     * This direction is used to determine the rotation of the clipboard to align to the curve. The horizontal slope
     * of the curve for a specific point is calculated by {@link Interpolation#get1stDerivative(double)}.
     * Subsequently, this angle between this vector, and the gradient vector is calculated, and the clipboard content
     * is rotated by that angle to follow the curve slope.
     * <p>
     * The default direction is a (1;0) vector (pointing in the positive x-direction).
     *
     * @return A vector representing the horizontal forward direction of the clipboard content
     */
    public Vector2 getDirection() {
        return direction;
    }

    /**
     * Paste the structure at the provided position on the curve. The position will be position-corrected if the
     * nodeCount provided to the constructor is bigger than 2.
     *
     * @param position The position on the curve. Must be between 0.0 and 1.0 (both inclusive)
     * @return The amount of blocks that have been changed
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
     * 2 dimensional "cross" product. cross2D(v1, v2) = |v1|*|v2|*sin(theta) or v1 X v2 taking Y to be 0
     */
    private double cross2D(Vector2 v1, Vector2 v2) {
        return v1.x() * v2.z() - v2.x() * v1.z();
    }

    /**
     * Paste structure at the provided position on the curve. The position will not be position-corrected
     * but will be passed directly to the interpolation algorithm.
     *
     * @param position The position on the curve. Must be between 0.0 and 1.0 (both inclusive)
     * @return The amount of blocks that have been changed
     * @throws MaxChangedBlocksException Thrown by WorldEdit if the limit of block changes for the {@link EditSession} has been reached
     */
    public int pastePositionDirect(double position) throws MaxChangedBlocksException {
        Preconditions.checkArgument(position >= 0);
        Preconditions.checkArgument(position <= 1);

        // Calculate position from spline
        Vector3 target = interpolation.getPosition(position);
        BlockVector3 blockTarget = target.toBlockPoint();
        Vector3 offset = target.subtract(target.floor());

        // Calculate rotation from spline

        Vector3 deriv = interpolation.get1stDerivative(position);
        Vector2 deriv2D = Vector2.at(deriv.x(), deriv.z()).normalize();
        double angle = Math.toDegrees(
                -Math.atan2(cross2D(direction, deriv2D), direction.dot(deriv2D))
        );

        angle = ((angle % 360) + 360) % 360; // Wrap to 360 degrees

        return pasteBlocks(blockTarget, offset, angle);
    }

    protected abstract int pasteBlocks(BlockVector3 target, Vector3 offset, double angle) throws MaxChangedBlocksException;

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

        double finalPosition = previousSection.uniStart + uniOffset;

        //Really rough fix, but fixes a bug with no visual artifacts so it's probably ok?
        //flexPosition very close to 1 causes outputs very slightly higher than 1 on rare occasions
        if (finalPosition > 1) {
            return 1;
        }

        return finalPosition;
    }

    private record Section(double uniStart, double uniLength, double flexStart, double flexLength) {

    }

}
