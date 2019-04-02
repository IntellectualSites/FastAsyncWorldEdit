package com.boydti.fawe.jnbt.anvil;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;

public class MCAClipboard {
    private final MCAQueue queue;
    private final CuboidRegion region;
    private final BlockVector3 origin;

    public MCAClipboard(MCAQueue queue, CuboidRegion region, BlockVector3 origin) {
        this.queue = queue;
        this.region = region;
        this.origin = origin;
    }

    public MCAQueue getQueue() {
        return queue;
    }

    public CuboidRegion getRegion() {
        return region;
    }

    public BlockVector3 getOrigin() {
        return origin;
    }
}
