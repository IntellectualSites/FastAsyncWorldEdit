package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import javax.annotation.Nullable;

/**
 * Restricts the
 */
public class SolidPlaneMask extends SolidBlockMask implements ResettableMask {

    private transient int mode = -1;
    private transient MutableBlockVector mutable = new MutableBlockVector();

    private int originX = Integer.MAX_VALUE, originY = Integer.MAX_VALUE, originZ = Integer.MAX_VALUE;

    public SolidPlaneMask(Extent extent) {
        super(extent);
    }

    @Override
    public boolean test(Vector vector) {
        switch (mode) {
            case -1:
                if (!super.test(vector)) {
                    return false;
                }
                originX = vector.getBlockX();
                originY = vector.getBlockY();
                originZ = vector.getBlockZ();
                mode = 0;
                Extent extent = getExtent();
                if (!extent.getBlockType(mutable.setComponents(originX - 1, originY, originZ)).getMaterial().isAir() && !extent.getBlockType(mutable.setComponents(originX + 1, originY, originZ)).getMaterial().isAir()) {
                    mode &= 1;
                }
                if (!extent.getBlockType(mutable.setComponents(originX, originY, originZ - 1)).getMaterial().isAir() && !extent.getBlockType(mutable.setComponents(originX, originY, originZ + 1)).getMaterial().isAir()) {
                    mode &= 4;
                }
                if (!extent.getBlockType(mutable.setComponents(originX, originY - 1, originZ + 1)).getMaterial().isAir() && !extent.getBlockType(mutable.setComponents(originX, originY + 1, originZ + 1)).getMaterial().isAir()) {
                    mode &= 2;
                }
                if (Integer.bitCount(mode) >= 3) {
                    return false;
                }
            case 0:
            case 1:
            case 2:
            case 4:
                if (!super.test(vector)) {
                    return false;
                }
                int original = mode;
                if (originX != vector.getBlockX()) {
                    mode &= 1;
                }
                if (originY != vector.getBlockY()) {
                    mode &= 2;
                }
                if (originZ != vector.getBlockZ()) {
                    mode &= 4;
                }
                if (Integer.bitCount(mode) >= 3) {
                    mode = original;
                    return false;
                }
            default:
                if (originX != vector.getBlockX() && (mode & 1) == 0) {
                    return false;
                }
                if (originZ != vector.getBlockZ() && (mode & 4) == 0) {
                    return false;
                }
                if (originY != vector.getBlockY() && (mode & 2) == 0) {
                    return false;
                }
                return super.test(vector);

        }
    }

    @Override
    public void reset() {
        mode = -1;
        mutable = new MutableBlockVector();
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }
}
