package com.sk89q.worldedit.world.block;

import com.boydti.fawe.beta.ITileInput;

public enum CompoundInput {
    NULL,
    CONTAINER() {
        @Override
        public BaseBlock get(BlockState state, ITileInput input, int x, int y, int z) {
            return state.toBaseBlock(input.getTag(x, y, z));
        }
    }

    ;
    public BaseBlock get(BlockState state, ITileInput input, int x, int y, int z) {
        return state.toBaseBlock();
    }
}