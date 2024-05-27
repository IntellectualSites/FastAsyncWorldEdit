package com.fastasyncworldedit.core.function.pattern;

import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import javax.annotation.Nullable;

public class BufferedPattern2D extends BufferedPattern {

    /**
     * Create a new {@link Pattern} instance
     *
     * @param actor  actor associated with the pattern
     * @param parent pattern to set
     */
    public BufferedPattern2D(Actor actor, Pattern parent) {
        super(actor, parent, null);
    }

    /**
     * Create a new {@link Pattern} instance
     *
     * @param actor    actor associated with the pattern
     * @param parent   pattern to set
     * @param region  anticipated area of the edit
     * @since 2.2.0
     */
    public BufferedPattern2D(Actor actor, Pattern parent, @Nullable Region region) {
        super(actor, parent, region);
    }

    @Override
    public boolean set(BlockVector3 pos) {
        return set.add(pos.x(), 0, pos.z());
    }

}
