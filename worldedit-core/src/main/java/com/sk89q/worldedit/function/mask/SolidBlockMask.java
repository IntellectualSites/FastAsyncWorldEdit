package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

public class SolidBlockMask extends BlockMask {
    public SolidBlockMask(Extent extent) {
        super(extent);
        add(state -> state.getMaterial().isSolid());
    }
}