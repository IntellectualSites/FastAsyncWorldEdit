package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;

import javax.annotation.Nullable;

/**
 * Restricts the
 */
public class SolidPlaneMask extends SolidBlockMask implements ResettableMask {

    private transient int mode = -1;
    private transient MutableBlockVector3 mutable = new MutableBlockVector3();

    private int originX = Integer.MAX_VALUE, originY = Integer.MAX_VALUE, originZ = Integer.MAX_VALUE;

    public SolidPlaneMask(Extent extent) {
        super(extent);
    }

    @Override
    public boolean test(BlockVector3 vector) {
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
                if (!extent.getBlock(mutable.setComponents(originX - 1, originY, originZ)).getMaterial().isAir() && !extent.getBlock(mutable.setComponents(originX + 1, originY, originZ)).getMaterial().isAir()) {
                    mode &= 1;
                }
                if (!extent.getBlock(mutable.setComponents(originX, originY, originZ - 1)).getMaterial().isAir() && !extent.getBlock(mutable.setComponents(originX, originY, originZ + 1)).getMaterial().isAir()) {
                    mode &= 4;
                }
                if (!extent.getBlock(mutable.setComponents(originX, originY - 1, originZ + 1)).getMaterial().isAir() && !extent.getBlock(mutable.setComponents(originX, originY + 1, originZ + 1)).getMaterial().isAir()) {
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
        mutable = new MutableBlockVector3();
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }
}
