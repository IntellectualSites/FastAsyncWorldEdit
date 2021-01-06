package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.Mask;

public class AirMask extends BlockMask {

    public AirMask(Extent extent) {
        super(extent);
        add(state -> state.getMaterial().isAir());
    }

    @Override
    public Mask copy() {
        return new AirMask(getExtent());
    }

    @Override
    public boolean replacesAir() {
        return true;
    }

}
