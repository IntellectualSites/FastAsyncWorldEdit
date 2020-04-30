package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class PatternTransform extends ResettableExtent {

    private final Pattern pattern;

    public PatternTransform(Extent parent, Pattern pattern) {
        super(parent);
        this.pattern = pattern;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block)
        throws WorldEditException {
        return pattern.apply(getExtent(), location, location);
    }
}
