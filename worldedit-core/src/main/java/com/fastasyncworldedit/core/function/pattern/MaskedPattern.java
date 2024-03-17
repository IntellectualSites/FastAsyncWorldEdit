package com.fastasyncworldedit.core.function.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

import java.util.Arrays;

public class MaskedPattern extends AbstractPattern implements StatefulPattern {

    private final Pattern primary;
    private final Pattern secondary;
    private final Mask mask;

    /**
     * Create a new {@link Pattern} instance
     *
     * @param mask      mask to use
     * @param primary   pattern if mask true
     * @param secondary pattern if mask false
     */
    public MaskedPattern(Mask mask, Pattern primary, Pattern secondary) {
        this.mask = mask;
        this.primary = primary;
        this.secondary = secondary;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        if (mask.test(position)) {
            return primary.applyBlock(position);
        }
        return secondary.applyBlock(position);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        if (mask.test(get)) {
            return primary.apply(extent, get, set);
        }
        return secondary.apply(extent, get, set);
    }

    @Override
    public StatefulPattern fork() {
        return new MaskedPattern(this.mask.copy(), this.primary.fork(), this.secondary.fork());
    }

}
