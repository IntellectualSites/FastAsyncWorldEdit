package com.boydti.fawe.object.pattern;

import com.boydti.fawe.beta.FilterBlock;
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
    private BlockVector3 origin;
    private final MutableBlockVector3 mutable = new MutableBlockVector3();

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
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        if (origin == null) {
            origin = set;
        }
        mutable.mutX((set.getX() - origin.getX()));
        mutable.mutY((set.getY() - origin.getY()));
        mutable.mutZ((set.getZ() - origin.getZ()));
        return pattern.apply(extent, get, mutable);
    }

    @Override
    public void reset() {
        origin = null;
    }
}
