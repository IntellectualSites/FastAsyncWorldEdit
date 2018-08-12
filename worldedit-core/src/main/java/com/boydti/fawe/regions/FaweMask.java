package com.boydti.fawe.regions;

import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.IDelegateRegion;
import com.sk89q.worldedit.regions.Region;

public class FaweMask implements IDelegateRegion {
    private final Region region;
    private String description = null;

    @Deprecated
    public FaweMask(final BlockVector pos1, final BlockVector pos2, final String id) {
        this(new CuboidRegion(pos1, pos2), id);
    }

    @Deprecated
    public FaweMask(final BlockVector pos1, final BlockVector pos2) {
        this(pos1, pos2, null);
        if ((pos1 == null) || (pos2 == null)) {
            throw new IllegalArgumentException("BlockVectors cannot be null!");
        }
    }

    public FaweMask(Region region, String id) {
        this.region = region;
        this.description = id;
    }

    @Override
    public Region getRegion() {
        return region;
    }

    public String getName() {
        return this.description;
    }

    public boolean isValid(FawePlayer player, FaweMaskManager.MaskType type) {
        return false;
    }

    @Override
    public Region clone() {
        throw new UnsupportedOperationException("Clone not supported");
    }
}