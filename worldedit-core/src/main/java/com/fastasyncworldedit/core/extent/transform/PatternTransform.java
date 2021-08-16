package com.fastasyncworldedit.core.extent.transform;

import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class PatternTransform extends ResettableExtent {

    private final Pattern pattern;

    /**
     * New instance
     *
     * @param parent  extent to set to
     * @param pattern pattern to apply
     */
    public PatternTransform(Extent parent, Pattern pattern) {
        super(parent);
        this.pattern = pattern;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block)
            throws WorldEditException {
        return pattern.apply(getExtent(), location, location);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block)
            throws WorldEditException {
        BlockVector3 vector3 = BlockVector3.at(x, y, z);
        return pattern.apply(extent, vector3, vector3);
    }

}
