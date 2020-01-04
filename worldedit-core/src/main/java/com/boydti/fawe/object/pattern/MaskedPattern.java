package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

public class MaskedPattern extends AbstractPattern {

    private final Pattern primary;
    private final Pattern secondary;
    private Mask mask;

    public MaskedPattern(Mask mask, Pattern primary, Pattern secondary) {
        this.mask = mask;
        this.primary = primary;
        this.secondary = secondary;
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        if (mask.test(position)) {
            return primary.apply(position);
        }
        return secondary.apply(position);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        if (mask.test(extent, get)) {
            return primary.apply(extent, get, set);
        }
        return secondary.apply(extent, get, set);
    }
}
