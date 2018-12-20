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

public class OffsetPattern extends AbstractPattern {

    private final int dx, dy, dz;
    private transient MutableBlockVector mutable = new MutableBlockVector();
    private final Pattern pattern;

    public OffsetPattern(Pattern pattern, int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.pattern = pattern;
    }

    @Override
    public BlockStateHolder apply(Vector position) {
        mutable.mutX((position.getX() + dx));
        mutable.mutY((position.getY() + dy));
        mutable.mutZ((position.getZ() + dz));
        return pattern.apply(mutable);
    }

    @Override
    public boolean apply(Extent extent, Vector set, Vector get) throws WorldEditException {
        mutable.mutX((get.getX() + dx));
        mutable.mutY((get.getY() + dy));
        mutable.mutZ((get.getZ() + dz));
        return pattern.apply(extent, set, mutable);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        mutable = new MutableBlockVector();
    }
}
