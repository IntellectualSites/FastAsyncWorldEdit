package com.boydti.fawe.object.pattern;

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import java.io.IOException;

public class ShadePattern extends AbstractPattern {
    private transient TextureUtil util;
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
        BlockType block = extent.getBlock(position).getBlockType();
        return (darken ? util.getDarkerBlock(block) : util.getLighterBlock(block)).getDefaultState().toBaseBlock();
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        util = Fawe.get().getCachedTextureUtil(true, 0, 100);
    }
}
