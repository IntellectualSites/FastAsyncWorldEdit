package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

public class NoYPattern extends AbstractPattern {

    private final Pattern pattern;
    private final MutableBlockVector3 mutable = new MutableBlockVector3();

    public NoYPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public BaseBlock apply(BlockVector3 pos) {
        mutable.mutX(pos.getX());
        mutable.mutZ(pos.getZ());
        return pattern.apply(mutable);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        mutable.mutX(get.getX());
        mutable.mutZ(get.getZ());
        return pattern.apply(extent, mutable, set);
    }
}
