package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.WorldEditException;

import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class MaskedPattern extends AbstractPattern {

    private final PatternExtent patternExtent;
    private final Pattern secondaryPattern;
    private Mask mask;

    public MaskedPattern(Mask mask, PatternExtent primary, Pattern secondary) {
        this.mask = mask;
        this.patternExtent = primary;
        this.secondaryPattern = secondary;
    }


    @Override
    public BlockStateHolder apply(BlockVector3 position) {
        patternExtent.setTarget(position);
        if (mask.test(position)) {
            return patternExtent.getAndResetTarget();
        }
        return secondaryPattern.apply(position);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 set, BlockVector3 get) throws WorldEditException {
        patternExtent.setTarget(get);
        if (mask.test(get)) {
            return patternExtent.getAndResetTarget(extent, set, get);
        }
        return secondaryPattern.apply(extent, set, get);
    }
}
