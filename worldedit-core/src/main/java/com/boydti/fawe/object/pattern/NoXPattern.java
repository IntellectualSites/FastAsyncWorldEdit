package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

public class NoXPattern extends AbstractPattern {

    private final Pattern pattern;
    private final MutableBlockVector3 mutable = new MutableBlockVector3();

    public NoXPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public BaseBlock apply(BlockVector3 pos) {
        mutable.mutY(pos.getY());
        mutable.mutZ(pos.getZ());
        return pattern.apply(mutable);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        mutable.mutY(get.getY());
        mutable.mutZ(get.getZ());
        return pattern.apply(extent, mutable, set);
    }
}
