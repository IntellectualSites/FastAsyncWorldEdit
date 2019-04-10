package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAFilterCounter;
import com.boydti.fawe.object.number.MutableLong;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

public class CountIdFilter extends MCAFilterCounter {
    private final boolean[] allowedId = new boolean[BlockTypes.size()];

    public CountIdFilter() {
    }

    public CountIdFilter addBlock(BlockType type) {
        allowedId[type.getInternalId()] = true;
        return this;
    }

    public CountIdFilter addBlock(BlockStateHolder block) {
        allowedId[block.getInternalBlockTypeId()] = true;
        return this;
    }

    @Override
    public MCAChunk applyChunk(MCAChunk chunk, MutableLong count) {
        // TODO FIXME
        for (int layer = 0; layer < chunk.ids.length; layer++) {
            int[] ids = chunk.ids[layer];
            if (ids != null) {
                for (int i : ids) {
                    if (allowedId[BlockTypes.getFromStateId(i).getInternalId()]) {
                        count.increment();
                    }
                }
            }
        }
        return null;
    }
}
