package com.boydti.fawe.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockMask;

public class LiquidMask extends BlockMask {

    public LiquidMask(Extent extent) {
        super(extent);
        add(state -> state.getMaterial().isLiquid());
    }

}
