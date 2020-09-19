package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.Mask;

public class LiquidMask extends BlockMask {

    public LiquidMask(Extent extent) {
        super(extent);
        add(state -> state.getMaterial().isLiquid());
    }

    public Mask clone() {
        return new LiquidMask(getExtent());
    }

}
