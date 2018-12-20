package com.boydti.fawe.jnbt.anvil;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.CuboidRegion;

public class MCAClipboard {
    private final MCAQueue queue;
    private final CuboidRegion region;
    private final Vector origin;

    public MCAClipboard(MCAQueue queue, CuboidRegion region, Vector origin) {
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

    public Vector getOrigin() {
        return origin;
    }
}
