package com.boydti.fawe.object.pattern;

import com.boydti.fawe.beta.DelegateFilterBlock;
import com.boydti.fawe.beta.FilterBlock;
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

public class NoZPattern extends AbstractPattern {

    private final Pattern pattern;

    public NoZPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    private transient MutableBlockVector3 mutable = new MutableBlockVector3();

    @Override
    public BaseBlock apply(BlockVector3 pos) {
        mutable.mutX((pos.getX()));
        mutable.mutY((pos.getY()));
        return pattern.apply(mutable);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        mutable.mutX((get.getX()));
        mutable.mutY((get.getY()));
        return pattern.apply(extent, mutable, set);
    }
}
