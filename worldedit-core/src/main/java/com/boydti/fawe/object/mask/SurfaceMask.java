package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockMaskBuilder;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.world.block.BlockTypes;

public class SurfaceMask extends AdjacentAnyMask {
    private final transient Extent extent;

    public SurfaceMask(Extent extent) {
        super(getMask(extent));
        this.extent = extent;
    }

    public static Mask getMask(Extent extent) {
        return new BlockMaskBuilder()
        .addTypes(BlockTypes.AIR, BlockTypes.CAVE_AIR, BlockTypes.VOID_AIR)
        .addAll(b -> !b.getMaterial().isMovementBlocker())
        .build(extent);
    }

    @Override
    public boolean test(Vector v) {
        return !getParentMask().test(v.getBlockX(), v.getBlockY(), v.getBlockZ()) && super.test(v);
    }
}