package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nullable;

public class SolidBlockMask extends BlockTypeMask {

    public static boolean[] getTypes() {
        boolean[] types = new boolean[BlockTypes.size()];
        for (BlockTypes type : BlockTypes.values) {
            types[type.getInternalId()] = type.getMaterial().isSolid();
        }
        return types;
    }

    public SolidBlockMask(Extent extent) {
        super(extent, getTypes());
    }
    @Override
    public boolean test(BlockVector3 vector) {
        Extent extent = getExtent();
        BlockState block = extent.getBlock(vector);
        return block.getBlockType().getMaterial().isMovementBlocker();
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }


}