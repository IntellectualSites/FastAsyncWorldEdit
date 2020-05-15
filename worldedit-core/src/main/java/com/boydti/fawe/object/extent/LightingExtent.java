package com.boydti.fawe.object.extent;

import com.boydti.fawe.beta.implementation.lighting.NullRelighter;
import com.boydti.fawe.beta.implementation.lighting.Relighter;
import com.boydti.fawe.object.RelightMode;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;

public class LightingExtent extends AbstractDelegateExtent {

    private final RelightMode relightMode;
    private final Relighter relighter;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public LightingExtent(Extent extent) {
        super(extent);
        this.relighter = NullRelighter.INSTANCE;
        this.relightMode = RelightMode.OPTIMAL;
    }

    public LightingExtent(Extent extent, Relighter relighter) {
        super(extent);
        this.relighter = relighter;
        this.relightMode = RelightMode.OPTIMAL;
    }

    public LightingExtent(Extent extent, Relighter relighter, RelightMode relightMode) {
        super(extent);
        this.relighter = relighter;
        this.relightMode = relightMode;
    }

    public Relighter getRelighter() {
        return relighter;
    }
}
