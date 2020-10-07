package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.BlockMaskBuilder;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;

public class SurfaceMask extends AdjacentAnyMask {

    public SurfaceMask(Extent extent) {
        super(getMask(extent));
    }

    public static AbstractExtentMask getMask(Extent extent) {
        return new BlockMaskBuilder()
                .addTypes(BlockTypes.AIR, BlockTypes.CAVE_AIR, BlockTypes.VOID_AIR)
                .addAll(b -> !b.getMaterial().isMovementBlocker())
                .build(extent);
    }

    @Override
    public boolean test(BlockVector3 v) {
        return !getParentMask().test(v.getBlockX(), v.getBlockY(), v.getBlockZ()) && super.test(v);
    }

    @Override
    public Mask copy() {
        // The mask is not mutable. There is no need to clone it.
        return this;
    }
}
