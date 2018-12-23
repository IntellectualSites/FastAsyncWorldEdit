package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.io.IOException;

public class NoXPattern extends AbstractPattern {

    private final Pattern pattern;
    private transient MutableBlockVector mutable = new MutableBlockVector();

    public NoXPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public BlockStateHolder apply(BlockVector3 pos) {
        mutable.mutY((pos.getY()));
        mutable.mutZ((pos.getZ()));
        return pattern.apply(mutable);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 set, BlockVector3 get) throws WorldEditException {
        mutable.mutY((get.getY()));
        mutable.mutZ((get.getZ()));
        return pattern.apply(extent, set, mutable);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        mutable = new MutableBlockVector();
    }
}
