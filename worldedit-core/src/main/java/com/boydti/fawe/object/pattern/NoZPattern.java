package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.WorldEditException;

import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.io.IOException;

public class NoZPattern extends AbstractPattern {

    private final Pattern pattern;

    public NoZPattern(Pattern pattern) {
        this.pattern = pattern;
    }

//    private transient MutableBlockVector mutable = new MutableBlockVector();

    @Override
    public BlockStateHolder apply(BlockVector3 pos) {
//        mutable.mutX((pos.getX()));
//        mutable.mutY((pos.getY()));
        return pattern.apply(pos);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 set, BlockVector3 get) throws WorldEditException {
//        mutable.mutX((get.getX()));
//        mutable.mutY((get.getY()));
        return pattern.apply(extent, set, get);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
//        mutable = new MutableBlockVector();
    }
}
