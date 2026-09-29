package com.fastasyncworldedit.core.world.block;

import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.IBlocks;
import com.fastasyncworldedit.core.queue.ITileInput;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

public enum CompoundInput {
    NULL,
    CONTAINER() {
        @Override
        public BaseBlock get(BlockState state, ITileInput input, int x, int y, int z) {
            return state.toBaseBlock(input.getTile(x, y, z));
        }

        @Override
        public BaseBlock get(final BlockState state, final IBlocks blocks, final int x, final int y, final int z) {
            final FaweCompoundTag tile = blocks.tile(x, y, z);
            return state.toBaseBlock(tile == null ? null : tile.linTag());
        }
    };

    @Deprecated(forRemoval = true, since = "2.11.2")
    public BaseBlock get(BlockState state, ITileInput input, int x, int y, int z) {
        return state.toBaseBlock();
    }

    public BaseBlock get(BlockState state, IBlocks blocks, int x, int y, int z) {
        return state.toBaseBlock();
    }
}
