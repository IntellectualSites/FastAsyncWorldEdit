package com.boydti.fawe.regions;

import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.IDelegateRegion;
import com.sk89q.worldedit.regions.Region;

public class FaweMask implements IDelegateRegion {
    private final Region region;

    @Deprecated
    public FaweMask(final BlockVector3 pos1, final BlockVector3 pos2) {
        this(new CuboidRegion(pos1, pos2));
    }

    public FaweMask(Region region) {
        this.region = region;
    }

    @Override
    public Region getRegion() {
        return region;
    }

    public boolean isValid(FawePlayer player, FaweMaskManager.MaskType type) {
        return false;
    }

    @Override
    public Region clone() {
        throw new UnsupportedOperationException("Clone not supported");
    }
}
