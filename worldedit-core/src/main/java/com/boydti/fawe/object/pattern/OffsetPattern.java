package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.io.IOException;

public class OffsetPattern extends AbstractPattern {

    private final int dx, dy, dz;
//    private transient MutableBlockVector3 mutable = new MutableBlockVector3();
    private final Pattern pattern;

    public OffsetPattern(Pattern pattern, int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.pattern = pattern;
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
//        mutable.mutX((position.getX() + dx));
//        mutable.mutY((position.getY() + dy));
//        mutable.mutZ((position.getZ() + dz));
//        return pattern.apply(mutable);
    	return pattern.apply(BlockVector3.at(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 set, BlockVector3 get) throws WorldEditException {
//        mutable.mutX((get.getX() + dx));
//        mutable.mutY((get.getY() + dy));
//        mutable.mutZ((get.getZ() + dz));
//        return pattern.apply(extent, set, mutable);
    	return pattern.apply(extent, set, BlockVector3.at(get.getX() + dx, get.getY() + dy, get.getZ() + dz));
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
//        mutable = new MutableBlockVector3();
    }
}
