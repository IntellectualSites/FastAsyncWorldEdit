package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.io.IOException;

public class NoYPattern extends AbstractPattern {

    private final Pattern pattern;

    public NoYPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    private transient MutableBlockVector mutable = new MutableBlockVector();

    @Override
    public BlockStateHolder apply(Vector pos) {
        mutable.mutX((pos.getX()));
        mutable.mutZ((pos.getZ()));
        return pattern.apply(mutable);
    }

    @Override
    public boolean apply(Extent extent, Vector set, Vector get) throws WorldEditException {
        mutable.mutX((get.getX()));
        mutable.mutZ((get.getZ()));
        return pattern.apply(extent, set, mutable);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        mutable = new MutableBlockVector();
    }
}
