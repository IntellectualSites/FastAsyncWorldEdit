package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.jnbt.anvil.MCAFilterCounter;
import com.boydti.fawe.object.number.MutableLong;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.function.pattern.Pattern;

// TODO FIXME
public class SetPatternFilter extends MCAFilterCounter {
    private final Pattern to;

    public SetPatternFilter(Pattern to) {
        this.to = to;
    }

    @Override
    public void applyBlock(int x, int y, int z, BaseBlock block, MutableLong count) {
//        BaseBlock newBlock = to.apply(x, y, z);
//        int currentId = block.getId();
//        if (FaweCache.hasNBT(currentId)) {
//            block.setNbtData(null);
//        }
//        block.setId(newBlock.getId());
//        block.setData(newBlock.getData());
//        count.increment();
    }
}
