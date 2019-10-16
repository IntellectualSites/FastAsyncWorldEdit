package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.jnbt.anvil.MCAFilterCounter;
import com.boydti.fawe.object.mask.FaweBlockMatcher;
import com.boydti.fawe.object.number.MutableLong;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

public class ReplaceSimpleFilter extends MCAFilterCounter {
    private final FaweBlockMatcher to;
    private final FaweBlockMatcher from;

    public ReplaceSimpleFilter(FaweBlockMatcher from, FaweBlockMatcher to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public void applyBlock(int x, int y, int z, BaseBlock block, MutableLong count) {
        if (from.apply(block)) {
            to.apply(block);
            count.increment();
        }
    }
}
