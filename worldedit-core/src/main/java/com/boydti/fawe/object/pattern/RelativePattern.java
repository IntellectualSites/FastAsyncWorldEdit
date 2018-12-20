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

public class RelativePattern extends AbstractPattern implements ResettablePattern {

    private final Pattern pattern;
    private transient Vector origin;
    private transient MutableBlockVector mutable = new MutableBlockVector();

    public RelativePattern(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public BlockStateHolder apply(Vector pos) {
        if (origin == null) {
            origin = new Vector(pos);
        }
        mutable.mutX((pos.getX() - origin.getX()));
        mutable.mutY((pos.getY() - origin.getY()));
        mutable.mutZ((pos.getZ() - origin.getZ()));
        return pattern.apply(mutable);
    }

    @Override
    public boolean apply(Extent extent, Vector set, Vector get) throws WorldEditException {
        if (origin == null) {
            origin = new Vector(get);
        }
        mutable.mutX((get.getX() - origin.getX()));
        mutable.mutY((get.getY() - origin.getY()));
        mutable.mutZ((get.getZ() - origin.getZ()));
        return pattern.apply(extent, set, mutable);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        mutable = new MutableBlockVector();
    }

    @Override
    public void reset() {
        origin = null;
    }
}
