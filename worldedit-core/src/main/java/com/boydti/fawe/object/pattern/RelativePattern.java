package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;

import java.io.IOException;

public class RelativePattern extends AbstractPattern implements ResettablePattern {

    private final Pattern pattern;
    private transient BlockVector3 origin;
    private transient MutableBlockVector3 mutable = new MutableBlockVector3();

    public RelativePattern(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public BaseBlock apply(BlockVector3 pos) {
        if (origin == null) {
            origin = pos;
        }
        mutable.mutX((pos.getX() - origin.getX()));
        mutable.mutY((pos.getY() - origin.getY()));
        mutable.mutZ((pos.getZ() - origin.getZ()));
        return pattern.apply(mutable);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 set, BlockVector3 get) throws WorldEditException {
        if (origin == null) {
            origin = get;
        }
        mutable.mutX((get.getX() - origin.getX()));
        mutable.mutY((get.getY() - origin.getY()));
        mutable.mutZ((get.getZ() - origin.getZ()));
        return pattern.apply(extent, set, mutable);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        mutable = new MutableBlockVector3();
    }

    @Override
    public void reset() {
        origin = null;
    }
}
