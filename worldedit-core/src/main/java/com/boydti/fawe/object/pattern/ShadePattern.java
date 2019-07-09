package com.boydti.fawe.object.pattern;

import com.boydti.fawe.beta.FilterBlock;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockType;


import static com.google.common.base.Preconditions.checkNotNull;

public class ShadePattern extends AbstractPattern {
    private final TextureUtil util;
    private final Extent extent;
    private final boolean darken;

    public ShadePattern(Extent extent, boolean darken, TextureUtil util) {
        checkNotNull(extent);
        this.extent = extent;
        this.util = util;
        this.darken = darken;
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        BlockType block = extent.getBlockType(position);
        return (darken ? util.getDarkerBlock(block) : util.getLighterBlock(block)).getDefaultState().toBaseBlock();
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        BlockType type = get.getBlock(extent).getBlockType();
        BlockType newType = (darken ? util.getDarkerBlock(type) : util.getLighterBlock(type));
        if (type != newType) {
            return set.setBlock(extent, newType.getDefaultState());
        }
        return false;
    }
}